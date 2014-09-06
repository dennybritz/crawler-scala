package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.{Timeout}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

object HostWorker {
  def props(service: ActorRef) = Props(classOf[HostWorker], service)
}

class HostWorker(service: ActorRef) extends Actor with HttpFetcher with ActorLogging {

  implicit val askTimeout = Timeout(5 seconds)

  /* Keeps track of all the processors a response goes through */
  val processors = scala.collection.mutable.ArrayBuffer.empty[ResponseProcessor]

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
    // We request the jobConf every time because it allows us to change
    // job properties on the fly. Possibly a bottleneck soon?
    // TODO: Store in locale cache (redis)
    service ? GetJob(jobId) map {
      case Some(jobConf: JobConfiguration) => jobConf
      case _ => 
        throw new IllegalArgumentException(
          s"Could not find job configuration for job=${jobId}")
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