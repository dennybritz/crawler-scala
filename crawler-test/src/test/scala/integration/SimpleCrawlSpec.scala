package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import org.blikk.crawler.processors._

class SimpleCrawlSpec extends IntegrationSuite("SimpleCrawlSpec") {

  describe("crawler") {
    
    it("should be able to crawl one link"){
      val appId = "SimpleCrawlSpec"
      implicit val (src, system) = createSource()
      implicit val mat = FlowMaterializer()
      
      val seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/1"))
      val frontier = FrontierSink.build(appId)

      src.to(Sink.foreach[CrawlItem] { item => 
        log.info("got data {}", item.toString) 
        assert(item.res.status.intValue === 200)
        probes(1).ref ! item.req.uri.toString
      }).run()

      Source(seeds).to(frontier).run()

      probes(1).within(5.seconds) {
        probes(1).expectMsg("http://localhost:9090/1")
      }
      probes(1).expectNoMsg()
      system.shutdown()
      system.awaitTermination()
    }

    it("should be able to extract and crawl multiple links") {
      val appId = "SimpleCrawlSpec"
      implicit val (in, system) = createSource()
      implicit val mat = FlowMaterializer()
      
      import system.dispatcher

      val seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/crawl/1"))
      val fLinkExtractor = RequestExtractor()
      val duplicateFilter = DuplicateFilter.buildUrlDuplicateFilter(
        List(WrappedHttpRequest.getUrl("http://localhost:9090/crawl/1")))
      val fLinkSender = Sink.foreach[CrawlItem] { item => 
        log.info("Got data for {}", item.req.uri.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val frontier = Flow[WrappedHttpRequest].map { item =>
        log.info("Adding to frontier: {}", item.uri.toString)
        item
      }.to(FrontierSink.build(appId))
      
      val graph = FlowGraph { implicit b =>
        val bcast = Broadcast[CrawlItem]
        val frontierMerge = Merge[WrappedHttpRequest]
        in ~> bcast 
        bcast ~> fLinkExtractor.via(duplicateFilter) ~> frontierMerge
        bcast ~> fLinkSender
        Source(seeds) ~> frontierMerge
        frontierMerge ~> frontier
      }.run()

      probes(1).within(10.seconds) {
        probes(1).receiveN(10).toSet shouldBe (1 to 10).map { num =>
          s"http://localhost:9090/crawl/${num}"}.toSet
      }

      probes(1).expectNoMsg()
      system.shutdown()
      system.awaitTermination()
    }

  }

}