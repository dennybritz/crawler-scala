package org.blikk.test

import org.blikk.crawler._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class RecrawlIntegrationSpec extends IntegrationSuite("SimpleCrawlIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should support recrawling") {
      val recrawlProcessor = RecrawlProcessor.withDelay("recrawler") { in =>
        if(in.req.uri.toString == "http://localhost:9090/2")
          Some(5000)
        else
          None
      }
      val processors = List(
        new LinkExtractor("link-extractor"),
        recrawlProcessor,
        new TestResponseProcessor(probes(1).ref)(systems(1)))
      val jobConf = JobConfiguration.empty("SimpleCrawlIntegrationSpecJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/1"),
          WrappedHttpRequest.getUrl("http://localhost:9090/2")),
        processors = processors
      )
      services(0) ! RunJob(jobConf)
      assert(probes(1).receiveN(2).toSet === Set("http://localhost:9090/1", 
        "http://localhost:9090/2"))
      probes(1).expectNoMsg()
      probes(1).expectMsg(5.seconds, "http://localhost:9090/2")
    }
  }

}