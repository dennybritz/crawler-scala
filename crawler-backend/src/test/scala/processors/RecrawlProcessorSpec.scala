package org.blikk.crawler.test

import org.scalatest._
import org.blikk.crawler._
import org.blikk.crawler.processors.RecrawlProcessor
import org.blikk.crawler.channels.FrontierChannelInput
import org.blikk.crawler.channels.FrontierChannelInput.AddToFrontierRequest

class RecrawlProcessorSpec extends FunSpec {

  val jobConf = JobConfiguration.empty("testJob")

  describe("RecrawlProcessor") {

    it("should work") {
      val req = WrappedHttpRequest.getUrl("http://localhost:9090")
      val res = WrappedHttpResponse.empty()
      val processor =  RecrawlProcessor.withDelay("testExtractor"){ in => Some(1000) }
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.head === AddToFrontierRequest(req, Some(1000), true))
    }

  }

}