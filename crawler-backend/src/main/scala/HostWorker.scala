package org.blikk.crawler

import org.blikk.crawler.channels.OutputputChannelPipeline
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.{Timeout}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import com.typesafe.config.ConfigFactory

object HostWorker {
  def props(service: ActorRef) = Props(classOf[HostWorker], service)
}

class HostWorker(service: ActorRef) extends Actor with HttpFetcher with ActorLogging {

  implicit val askTimeout = Timeout(5 seconds)

  /* Keeps track of all the processors a response goes through */
  val processors = scala.collection.mutable.ArrayBuffer.empty[ResponseProcessor]
  /* Handles the output */
  val outputChannels = new OutputputChannelPipeline()

  val workerBehavior : Receive = {
    case FetchRequest(req: WrappedHttpRequest, jobId) =>
      log.debug("requesting url=\"{}\"", req.req.uri)
      context.system.eventStream.publish(JobEvent(jobId, req))
      dispatchHttpRequest(req, jobId, self)
    case msg @ FetchResponse(res, req, jobId) =>
      log.debug("processing response for url=\"{}\"", req.req.uri)
      context.system.eventStream.publish(JobEvent(jobId, msg))
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
    // TODO: Get job stats + JobConfig    
    val initialProcessorInput = new ResponseProcessorInput(res, req, jobConf, Map.empty, Map.empty)
    val finalPinuput = jobConf.processors.foldLeft(initialProcessorInput) { (pin, proc) =>
      pin.copy(context = pin.context ++ proc.process(pin))
    }
    log.debug("final context: {}", finalPinuput.context.toString)
    outputChannels.process(finalPinuput.context)
  }

}