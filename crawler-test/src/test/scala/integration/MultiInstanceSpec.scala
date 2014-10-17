package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.app._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class MultiInstanceSpec extends IntegrationSuite("MultiInstanceSpec") {

  describe("crawler") {
    
    it("should support multiple app instances"){

      // Create two apps
      val streamContext1 = createStreamContext()
      val streamContext2 = createStreamContext()
      val frontierSink = FrontierSink.build()(streamContext1)

      // Run the same graph in each context
      // Data should be shared
      List(streamContext1, streamContext2).foreach { streamContext =>
        import streamContext.{materializer, system}    
        streamContext.flow.connect(ForeachDrain[CrawlItem] { item => 
          item.res.status.intValue shouldBe 200
          probes(1).ref ! item.req.uri.toString
        }).run()
      }
      
      // Request 40 pages
      val seeds = (1 to 40).map { i =>
        WrappedHttpRequest.getUrl(s"http://localhost:9090/${i}") 
      }.toList

      Source(seeds).connect(frontierSink).run()(streamContext1.materializer)

      // Expect to receive 40 results, no more
      probes(1).receiveN(40, 20.seconds).map(_.toString).sorted shouldBe 
        (1 to 40).map(i => s"http://localhost:9090/${i}").sorted 
      probes(1).expectNoMsg()

      // Clean up
      streamContext1.shutdown()
      streamContext2.shutdown()
      
    }
  }
}