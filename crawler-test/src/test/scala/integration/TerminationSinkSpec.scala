package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import org.blikk.crawler.processors._

class TerminationSinkSpec extends IntegrationSuite("TerminationSinkSpec") {

  describe("crawler") {

    it("should terminate on termination conditions") {
      implicit val (in, system) = createSource()
      implicit val mat = FlowMaterializer()
      import system.dispatcher

      val seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/crawl/1"))
      val fLinkExtractor = RequestExtractor()
      val fLinkSender = Flow[CrawlItem].map { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
        item
      }
      val frontier = FrontierSink.build(appId)
      
      val graph = FlowGraph { implicit b =>
        val frontierMerge = Merge[WrappedHttpRequest]
        val bcast = Broadcast[CrawlItem]
        val fTerminationSink = TerminationSink.build(_.numFetched >= 5)
        in ~> bcast ~> fLinkExtractor ~> frontierMerge
        bcast ~> fLinkSender.to(fTerminationSink)
        Source(seeds) ~> frontierMerge
        frontierMerge ~> frontier
      }.run()


      probes(1).within(5.seconds) {
        probes(1).receiveN(5).toSet shouldBe (1 to 5).map { num =>
          s"http://localhost:9090/crawl/${num}"}.toSet
      }
      probes(1).expectNoMsg()
    }

  }

}