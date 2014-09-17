package org.blikk.test

import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.client._

class SimpleCrawlIntegrationSpec extends IntegrationSuite("SimpleCrawlIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should be able to crawl links from the same domain"){
      import streamContext.{materializer, system}
      
      streamContext.flow.foreach{ item => 
        log.info("{}", item.toString) 
        assert(item.res.status.intValue === 200)
        probes(1).ref ! item.req.uri.toString
      }

      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/1")
      probes(1).expectMsg("http://localhost:9090/1")
      streamContext.shutdown()
    }
  }

}