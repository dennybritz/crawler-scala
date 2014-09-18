package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.app._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import org.blikk.crawler.processors._

class SimpleCrawlSpec extends IntegrationSuite("SimpleCrawlIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should be able to crawl one link"){

      implicit val streamContext = createStreamContext()
      import streamContext.{materializer, system}
      
      streamContext.flow.withSink(ForeachSink { item => 
        log.info("{}", item.toString) 
        assert(item.res.status.intValue === 200)
        probes(1).ref ! item.req.uri.toString
      }).run()

      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/1")
      probes(1).expectMsg("http://localhost:9090/1")
      probes(1).expectNoMsg()
      streamContext.shutdown()
    }

    it("should be able to extract and crawl multiple links") {
      implicit val streamContext = createStreamContext()
      import streamContext.{materializer, system}
      import system.dispatcher

      val in = streamContext.flow
      val fLinkExtractor = RequestExtractor.build()
      val fLinkSender = ForeachSink[CrawlItem] { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val stats = StatsCollector.build()

      val graph = FlowGraph { implicit b =>
        val bcast = Broadcast[CrawlItem]
        in ~> bcast ~> fLinkExtractor ~> FrontierSink.build()
        bcast ~> fLinkSender
        bcast ~> stats 
      }.run()

      stats.future(graph).onSuccess { case stats: CrawlStats => log.info(stats.toString) }

      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/links/1")
      probes(1).receiveN(3).toSet should === (Set("http://localhost:9090/links/1", 
              "http://localhost:9090/links/2", "http://localhost:9090/links/3"))
      probes(1).expectNoMsg()
      streamContext.shutdown()
    }


  }

}