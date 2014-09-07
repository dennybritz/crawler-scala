package org.blikk.crawler.channels

import org.blikk.crawler.WrappedHttpRequest
import org.blikk.crawler.ProcessorOutput

case class RabbitMQChannelInput(
  connectionString: String, 
  queue: String,
  messages: List[String]) extends ProcessorOutput

case class FrontierChannelInput(
  jobId: String,
  newRequests: Seq[WrappedHttpRequest]
) extends ProcessorOutput