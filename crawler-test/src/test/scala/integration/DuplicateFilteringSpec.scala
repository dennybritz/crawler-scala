package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.app._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import org.blikk.crawler.processors._

class DuplicateFilteringSpec extends IntegrationSuite("DuplicateFilteringSpec") {

  describe("crawler") {
    
    it("should be able to filter duplicate links") {
      implicit val streamContext = createStreamContext()
      import streamContext.{materializer, system}

      val in = streamContext.flow
      val fLinkExtractor = RequestExtractor()
      val fLinkSender = Sink.foreach[CrawlItem] { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val dupFilter = DuplicateFilter.buildUrlDuplicateFilter()
      val seeds = List(
        WrappedHttpRequest.getUrl("http://localhost:9090/links/1"),
        WrappedHttpRequest.getUrl("http://localhost:9090/links/1")
      )
      val frontier = FrontierSink.build(streamContext.appId)

      FlowGraph { implicit b =>
        val frontierMerge = Merge[WrappedHttpRequest]
        val bcast = Broadcast[CrawlItem]        
        in ~> bcast ~> fLinkExtractor.via(dupFilter) ~> frontierMerge
        bcast ~> fLinkSender
        Source(seeds) ~> frontierMerge 
        frontierMerge ~> frontier
      }.run()

      
      probes(1).receiveN(4).toSet should === (Set("http://localhost:9090/links/1", 
              "http://localhost:9090/links/2", "http://localhost:9090/links/3"))
      probes(1).expectNoMsg()
      streamContext.shutdown()
    }

  }

}
