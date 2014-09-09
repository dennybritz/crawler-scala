package org.blikk.crawler.channels

import akka.actor._
import org.blikk.crawler._

class JobOutputChannel(implicit system: ActorSystem)
  extends OutputChannel[JobChannelInput] with Logging {

  def serviceActor = system.actorSelection("/user/crawlService")

  def pipe(input: JobChannelInput, jobConf: JobConfiguration, jobStats: Map[String, Int]) : Unit = {
    input.action match {
      case JobChannelInput.Actions.Terminate => 
        serviceActor ! TerminateJob(jobConf.jobId)
      case x =>
        log.warn("Unhandled job action: {}", x)
    }
  }

}