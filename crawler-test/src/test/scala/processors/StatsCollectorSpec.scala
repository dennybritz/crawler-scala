package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import org.blikk.crawler.processors.{CrawlStats, StatsCollector}
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._
import spray.http.{HttpResponse, HttpEntity}

class StatsCollectorSpec extends AkkaSingleNodeSpec("StatsCollectorSpec") {

  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  def itemWithBytes(numBytes: Int) = {
    val dataEntity = HttpEntity(Array.ofDim[Byte](numBytes))
    CrawlItem(WrappedHttpRequest.empty, HttpResponse(entity=dataEntity), "testJob")
  }

  describe("Stats Collector") {
    
    it("should correctly aggregate items"){
      val data = List(itemWithBytes(100), itemWithBytes(200), itemWithBytes(550))
      val scSink = StatsCollector.build()
      val resultFuture = Source(data).runWith(scSink)
      val finalStats = Await.result(resultFuture, 1.second)
      finalStats.numFetched shouldBe 3
      finalStats.numBytesFetched shouldBe 850
    }

  }

}