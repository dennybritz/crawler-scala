package org.blikk.crawler.processors

import org.blikk.crawler._
import org.blikk.crawler.channels.FrontierChannelInput
import spray.http._
import spray.http.StatusCodes._

class StatusCodeProcessor(val name: String)  extends ResponseProcessor with Logging {

  def process(in: ResponseProcessorInput) : Map[String, ProcessorOutput] = {
    
    val output : Option[FrontierChannelInput] = in.res.status match {
      case code : Success => None
      case code : Redirection => 
        val newRequests = in.res.headers.map {
          case HttpHeaders.Location(url) => 
             WrappedHttpRequest.getUrl(url.toString).withProvenance(in.req)
        }
        Some(FrontierChannelInput(newRequests))
      case code : ServerError =>
        log.warn("error fetching {} ({}): {}", in.req.uri.toString, in.req.uuid, in.res)
        None
      case code : ClientError =>
        log.warn("error fetching {} ({}): {}", in.req.uri.toString, in.req.uuid, in.res)
        None
      case otherCode => 
        log.warn("unhandled HTTP status code: {}", otherCode.toString)
        None
    }

    output match {
      case Some(out) => Map(this.name -> out)
      case None => Map.empty
    }


  }

}