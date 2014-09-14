package org.blikk.crawler.channels

import akka.actor._
import org.blikk.crawler.{Logging, RouteFetchRequest, AddToFrontier, JobConfiguration}
import akka.event.Logging

class FrontierOutputChannel(serviceActor: ActorRef)(implicit system: ActorSystem)
  extends OutputChannel[FrontierChannelInput] {

  val log = Logging.getLogger(system, this)

  def pipe(input: FrontierChannelInput, jobConf: JobConfiguration, jobStats: Map[String, Int]) : Unit = {
    // Send each request to the local service master
    input.newRequests.foreach { req =>
      log.info(s"Adding URL to frontier: ${req.req.uri.toString}")
      serviceActor ! AddToFrontier(req.req, jobConf.jobId, req.scheduledTime, req.ignoreDeduplication)
    }

  }

}