package org.blikk.crawler.processors

import akka.stream.scaladsl2.{ForeachSink}
import org.blikk.crawler.app.StreamContext
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}

object FrontierSink {

  /** 
    * Builds a sink that sends incoming requests to the crawler frontier
    */ 
  def build()(implicit ctx: StreamContext[_]) : ForeachSink[WrappedHttpRequest] = {
    ForeachSink { ctx.api ! _ }
  }
}