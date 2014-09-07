package org.blikk.crawler.channels

import akka.actor._
import org.blikk.crawler.{Logging, RouteFetchRequest, FetchRequest}

class FrontierOutputChannel(implicit system: ActorSystem)
  extends OutputChannel[FrontierChannelInput] with Logging {

  def serviceActor = system.actorSelection("/user/crawlService")

  def pipe(input: FrontierChannelInput) : Unit = {
    // Send each request to the local service master
    input.newRequests.foreach { req =>
      log.debug(s"Adding URL to frontier: ${req.uri.toString}")
      serviceActor ! RouteFetchRequest(FetchRequest(req, input.jobId))
    }

  }

}