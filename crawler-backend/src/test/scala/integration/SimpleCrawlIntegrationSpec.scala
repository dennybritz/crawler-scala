package org.blikk.test

import org.blikk.crawler._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class SimpleCrawlIntegrationSpec extends IntegrationSuite("SimpleCrawlIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should be able to crawl links from the same domain"){
      val processors = List(
        new LinkExtractor("link-extractor"),
        new TestResponseProcessor(probes(1).ref)(systems(1)))
      val jobConf = JobConfiguration.empty("SimpleCrawlIntegrationSpecJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/links/1")),
        processors = processors
      )

      services(0) ! RunJob(jobConf)
      assert(probes(1).receiveN(3, 5.seconds).toSet === Set("http://localhost:9090/links/1", "http://localhost:9090/links/2", 
        "http://localhost:9090/links/3"))
      probes.foreach(_.expectNoMsg())
    }
  }

}