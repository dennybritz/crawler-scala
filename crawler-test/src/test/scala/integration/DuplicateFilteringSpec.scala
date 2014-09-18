package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.app._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import org.blikk.crawler.processors._

class DuplicateFilteringSpec extends IntegrationSuite("DuplicateFilteringSpec") {

  describe("crawler") {
    
    it("should be able to filter duplicate links") {
      implicit val streamContext = createStreamContext()
      import streamContext.{materializer, system}

      val in = streamContext.flow
      val fLinkExtractor = RequestExtractor.build()
      val fLinkSender = ForeachSink[CrawlItem] { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val dupFilter = DuplicateFilter.buildUrlDuplicateFilter()

      FlowGraph { implicit b =>
        val bcast = Broadcast[CrawlItem]        
        in ~> bcast ~> fLinkExtractor.append(dupFilter) ~> FrontierSink.build()
        bcast ~> fLinkSender
      }.run()

      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/links/1")
      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/links/1")
      probes(1).receiveN(4).toSet should === (Set("http://localhost:9090/links/1", 
              "http://localhost:9090/links/2", "http://localhost:9090/links/3"))
      probes(1).expectNoMsg()
      streamContext.shutdown()
    }

  }

}
