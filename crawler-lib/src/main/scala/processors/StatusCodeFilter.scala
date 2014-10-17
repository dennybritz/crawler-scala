package org.blikk.crawler.processors

import org.blikk.crawler._
import spray.http.StatusCodes._
import akka.stream.scaladsl2.{Flow}

object StatusCodeFilter {

  /* Build a status code filtering flow */
  def build() : Flow[CrawlItem, CrawlItem] =  {
    val scf = new StatusCodeFilter()
    Flow[CrawlItem].filter(scf.filter)
  }

}

/**
  * Filters crawler responses based on the HTTP status code 
  * Items with success (2xx) or redirection (3xx) codes pass through the filter.
  * Items with client (4xx) or server error (5xx) codes are filtered out.
  * Other unknown status codes are filtered out.
  */ 
class StatusCodeFilter extends Logging {
  def filter(item: CrawlItem) : Boolean = item.res.status match {
    case code : Success => true
    case code : Redirection => true
    case code : ServerError =>
      log.warn(s"error fetching ${item.req.uri}: ${item.res.toString}")
      false
    case code : ClientError =>
      log.warn(s"error fetching ${item.req.uri}: ${item.res.toString}")
      false
    case otherCode => 
      log.warn(s"unhandled HTTP status code: ${otherCode.toString}")
      false
  }
}
