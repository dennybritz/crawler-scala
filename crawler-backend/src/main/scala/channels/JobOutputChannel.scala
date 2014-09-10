package org.blikk.crawler.channels

import akka.routing.Broadcast
import akka.actor._
import org.blikk.crawler._

class JobOutputChannel(serviceActor: ActorRef)(implicit system: ActorSystem)
  extends OutputChannel[JobChannelInput] with Logging {

  def pipe(input: JobChannelInput, jobConf: JobConfiguration, jobStats: Map[String, Int]) : Unit = {
    input.action match {
      case JobChannelInput.Actions.Stop => 
        serviceActor ! Broadcast(StopJob(jobConf.jobId))
      case x =>
        log.warn("Unhandled job action: {}", x)
    }
  }

}