package org.blikk.crawler.test

import org.scalatest._
import org.blikk.crawler._
import org.blikk.crawler.processors.RecrawlProcessor
import org.blikk.crawler.channels.FrontierChannelInput
import org.blikk.crawler.channels.FrontierChannelInput.AddToFrontierRequest

class RecrawlProcessorSpec extends FunSpec with Matchers {

  val jobConf = JobConfiguration.empty("testJob")

  describe("RecrawlProcessor") {

    it("should work") {
      val req = WrappedHttpRequest.getUrl("http://localhost:9090")
      val res = WrappedHttpResponse.empty()
      val delay = 1000
      val processor =  RecrawlProcessor.withDelay("testExtractor"){ in => Some(delay) }
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      val output = result("testExtractor").asInstanceOf[FrontierChannelInput]
      assert(output.newRequests.head.scheduledTime.get === ((System.currentTimeMillis + delay) +- 50))
    }

  }

}