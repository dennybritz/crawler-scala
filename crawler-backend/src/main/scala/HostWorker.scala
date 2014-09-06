package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.{Timeout}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

object HostWorker {
  def props(host: String) = Props(classOf[HostWorker], host)
}

class HostWorker(host: String) extends Actor with HttpFetcher with ActorLogging {

  implicit val askTimeout = Timeout(5 seconds)

  /* Keeps track of all the processors a response goes through */
  val processors = scala.collection.mutable.ArrayBuffer.empty[ResponseProcessor]
  /* Caches job configurations so we don't need to ask our parent every time */
  val jobCache = MutableMap[String, JobConfiguration]()

  override def preStart() : Unit = {
    log.info("started for host={}", host)
    initializeHttpFetcher(host)
  }

  val workerBehavior : Receive = {
    case FetchRequest(req: WrappedHttpRequest, jobId) =>
      log.debug("requesting url=\"{}\"", req.req.uri)
      dispatchHttpRequest(req, jobId, self)
    case FetchResponse(res, req, jobId) =>
      log.debug("processing response for url=\"{}\"", req.req.uri)
      getJobConf(jobId) onComplete {
        case Success(jobConf) => processResponse(res, req, jobConf)
        case Failure(err) => log.error(err.toString)
      }
    case AddProcessor(proc, _) =>
      log.debug("adding processor={}", proc.name)
      processors += proc
  }

  def receive = workerBehavior

  def getJobConf(jobId: String) : Future[JobConfiguration] = {
    jobCache.get(jobId) match {
      case Some(jobConf) => return Promise.successful(jobConf).future
      case None =>
        // Request the job configuration from the parent
        // This is probably the first time we're dealing with this job
        context.parent ? GetJob(jobId) map {
          case Some(jobConf: JobConfiguration) => 
            jobCache.put(jobId, jobConf)
            jobConf
          case _ => 
            throw new IllegalArgumentException(
              s"Could not find job configuration for job=${jobId}")
        }
    }
  }

  def processResponse(res: WrappedHttpResponse, req: WrappedHttpRequest, 
    jobConf: JobConfiguration) : Unit = {
    val finalContext = jobConf.processors.foldLeft(Map.empty[String,Any]) { (ctx, proc) =>
      proc.process(res, req, jobConf, ctx)
    }
    log.debug("final context: {}", finalContext.toString)
  }

}