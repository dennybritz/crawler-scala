package org.blikk.crawler.processors

import org.blikk.crawler._
import spray.http._
import spray.http.StatusCodes._
import akka.stream.scaladsl2._

object StatusCodeFilter {
  def build() : ProcessorFlow[CrawlItem, CrawlItem] =  {
    val filter = new StatusCodeFilter()
    FlowFrom[CrawlItem].filter(filter.runThrough)
  }
}

class StatusCodeFilter extends Logging {
  def runThrough(item: CrawlItem) : Boolean = item.res.status match {
    case code : Success => true
    case code : Redirection => true
    case code : ServerError =>
      log.warn("error fetching {} ({}): {}", item.req.uri.toString, item.req.uuid, item.res)
      false
    case code : ClientError =>
      log.warn("error fetching {} ({}): {}", item.req.uri.toString, item.req.uuid, item.res)
      false
    case otherCode => 
      log.warn("unhandled HTTP status code: {}", otherCode.toString)
      false
  }
}
