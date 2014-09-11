package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.throttle._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.blikk.crawler._
import org.blikk.crawler.channels.OutputputChannelPipeline
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}

object HostWorker {
  def props(service: ActorRef, statsCollector: ActorRef, 
    rate: Throttler.Rate = Throttler.Rate(4, 1.second)) = {
    Props(classOf[HostWorker], service, statsCollector, rate)
  }
}

class HostWorker(service: ActorRef, statsCollector: ActorRef, val throttleRate: Throttler.Rate) 
  extends Actor with HttpFetcher with ActorLogging {

  implicit val askTimeout = Timeout(10 seconds)

  /* Keeps track of all the processors a response goes through */
  val processors = scala.collection.mutable.ArrayBuffer.empty[ResponseProcessor]
  /* Handles the output */
  val outputChannels = new OutputputChannelPipeline(service)

  val workerBehavior : Receive = {
    case FetchRequest(req: WrappedHttpRequest, jobId) =>
      log.info("requesting url=\"{}\" ({})", req.req.uri, req.uuid)
      context.system.eventStream.publish(JobEvent(jobId, req))
      dispatchHttpRequest(req, jobId, self)
    case msg @ FetchResponse(res, req, jobId) =>
      log.info("processing response for url=\"{}\" ({})", req.req.uri, req.uuid)
      context.system.eventStream.publish(JobEvent(jobId, msg))
      processResponse(jobId, res, req)
  }

  def receive = workerBehavior

  def processResponse(jobId: String, res: WrappedHttpResponse, req: WrappedHttpRequest) : Unit = {
    // Ask for the job config and job statistics
    val jobConfigFuture = service ? GetJob(jobId)
    val jobStatsFuture =  statsCollector ? GetJobEventCounts(jobId)
    val initialProcessorInputF = for {
      jobConf <- jobConfigFuture.mapTo[JobConfiguration]
      jobStats <- jobStatsFuture.mapTo[JobStats].map(_.eventCounts)
    } yield ResponseProcessorInput(res, req, jobConf, jobStats, Map.empty)

    initialProcessorInputF onComplete {
      case Success(initialInput) =>
        val result = initialInput.jobConf.processors.foldLeft(initialInput) { (pin, proc) =>
          pin.copy(context = pin.context ++ proc.process(pin))
        }
        log.debug("final context: {}", result.context.toString)
        outputChannels.process(result)
      case Failure(err) =>
        log.error(err.toString)
    }
  }

}