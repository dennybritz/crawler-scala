package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class MultiInstanceSpec extends IntegrationSuite("MultiInstanceSpec") {

  describe("crawler") {
    
    it("should support multiple app instances"){

      val appId = "MultiInstanceSpec"

      // Create two apps
      val (source1, system1) = createSource()
      val (source2, system2) = createSource()
      val frontierSink = FrontierSink.build(appId)(system1)

      // Run the same graph in each context
      // Data should be shared
      List((source1, system1), (source2, system2)).foreach { case(source, system) =>
        implicit val mat = FlowMaterializer()(system)
        source.to(Sink.foreach[CrawlItem] { item => 
          item.res.status.intValue shouldBe 200
          probes(1).ref ! item.req.uri.toString
        }).run()
      }
      
      // Request 40 pages
      val seeds = (1 to 40).map { i =>
        WrappedHttpRequest.getUrl(s"http://localhost:9090/${i}") 
      }.toList

      Source(seeds).to(frontierSink).run()(FlowMaterializer()(system1))

      // Expect to receive 40 results, no more
      probes(1).receiveN(40, 20.seconds).map(_.toString).sorted shouldBe 
        (1 to 40).map(i => s"http://localhost:9090/${i}").sorted 
      probes(1).expectNoMsg()
      
    }
  }
}