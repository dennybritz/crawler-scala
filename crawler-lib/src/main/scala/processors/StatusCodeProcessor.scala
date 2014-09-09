package org.blikk.crawler.processors

import org.blikk.crawler._
import org.blikk.crawler.channels.FrontierChannelInput
import spray.http._
import spray.http.StatusCodes._

class StatusCodeProcessor(val name: String)  extends ResponseProcessor with Logging {

  def process(res: WrappedHttpResponse, req: WrappedHttpRequest, jobConf: JobConfiguration, 
    context: Map[String, ProcessorOutput]) : Map[String, ProcessorOutput] = {
    
    val output : Option[FrontierChannelInput] = res.status match {
      case code : Success => None
      case code : Redirection => 
        val newRequests = res.headers.map {
          case HttpHeaders.Location(url) => 
             WrappedHttpRequest.getUrl(url.toString).withProvenance(req)
        }
        Some(FrontierChannelInput(jobConf.jobId, newRequests))
      case code : ServerError =>
        log.warn("error fetching {} ({}): {}", req.uri.toString, req.uuid, res)
        None
      case code : ClientError =>
        log.warn("error fetching {} ({}): {}", req.uri.toString, req.uuid, res)
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