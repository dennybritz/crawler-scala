package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.jsoup._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import spray.http.Uri

object FrontierSink {
  def build()(implicit ctx: StreamContext[CrawlItem]) : ForeachSink[WrappedHttpRequest] = {
    
    import ctx.system
    lazy val log = akka.event.Logging.getLogger(ctx.system, this)

    ForeachSink { newReq =>  
      log.debug("Sending to frontier: {}", newReq)
      ctx.api ! newReq
    }
  }
}