package org.blikk.crawler.test

import org.scalatest._
import org.blikk.crawler._
import org.blikk.crawler.processors.JobManagementProcessor
import org.blikk.crawler.channels.JobChannelInput
import spray.http.Uri

class JobManagementProcessorSpec extends FunSpec {

  val jobConf = JobConfiguration.empty("testJob")

  describe("JobManagementProcessor") {

    it("should work") {
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty()
      val processor = JobManagementProcessor.terminateWhen("testExtractor"){ in =>
        in.req.host == "localhost"
      }
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[JobChannelInput])
      assert(result("testExtractor").asInstanceOf[JobChannelInput].action 
        === JobChannelInput.Actions.Stop)
    }

  }

}