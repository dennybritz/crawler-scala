package org.blikk.test

import org.blikk.crawler._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class JobTerminationIntegrationSpec extends IntegrationSuite("JobTerminationIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should support recrawling") {
      val processors = List(
        JobManagementProcessor.terminateWhen("jobterm") { 
          in => in.req.uri.toString == "http://localhost:9090/links/1"
        },
        new LinkExtractor("link-extractor"),
        new TestResponseProcessor(probes(1).ref)(systems(1)))
      val jobConf = JobConfiguration.empty("SimpleCrawlIntegrationSpecJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/links/1")),
        processors = processors
      )
      services(0) ! RunJob(jobConf)
      assert(probes(1).receiveN(1).toSet === Set("http://localhost:9090/links/1"))
      probes(1).expectNoMsg()
    }
  }

}