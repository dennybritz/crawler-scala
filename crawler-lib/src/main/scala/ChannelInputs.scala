package org.blikk.crawler.channels

import org.blikk.crawler.WrappedHttpRequest
import org.blikk.crawler.ProcessorOutput

case class RabbitMQChannelInput(
  connectionString: String, 
  queue: String,
  messages: List[String]) extends ProcessorOutput

case class FrontierChannelInput(
  newRequests: Seq[WrappedHttpRequest]
) extends ProcessorOutput

object JobChannelInput {
  case class JobAction(name: String)
  object Actions {
    val Terminate = JobAction("Terminate")
    val Stop = JobAction("Stop")
  }
}

case class JobChannelInput(
  action: JobChannelInput.JobAction
) extends ProcessorOutput