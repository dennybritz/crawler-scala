package org.blikk.crawler.processors

import org.blikk.crawler._
import spray.http.Uri
import org.blikk.crawler.channels.FrontierChannelInput
import org.blikk.crawler.channels.FrontierChannelInput.AddToFrontierRequest


object RecrawlProcessor {
  def withDelay(name: String)(func: (ResponseProcessorInput => Option[Long])) = {
    new RecrawlProcessor(name)(func)
  }
}

class RecrawlProcessor(val name: String)(func: (ResponseProcessorInput => Option[Long])) 
  extends ResponseProcessor {

  def process(in: ResponseProcessorInput) : Map[String, ProcessorOutput] = {
    func(in) match {
      case Some(recrawlDelay) => 
        val scheduledTime = System.currentTimeMillis + recrawlDelay
        val newReq = AddToFrontierRequest(in.req, Some(scheduledTime), true)
        Map(name -> FrontierChannelInput(List(newReq)))
      case None => Map.empty
    }
  }
}