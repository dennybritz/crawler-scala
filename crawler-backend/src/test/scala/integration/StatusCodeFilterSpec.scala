package org.blikk.test.integration

import org.blikk.test._
import org.blikk.crawler._
import scala.concurrent.duration._
import org.blikk.crawler.app._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import org.blikk.crawler.processors._

class StatusCodeFilterSpec extends IntegrationSuite("StatusCodeFilterIntegrationSpec") {

  describe("A distributed crawler") {
    
    it("should be able to filter results by status code") {
      implicit val streamContext = createStreamContext()
      import streamContext.{materializer, system}

      val fLinkSender = ForeachSink[CrawlItem] { item => 
        log.info("{}", item.toString) 
        probes(1).ref ! item.req.uri.toString
      }
      val statusCodeFilter = StatusCodeFilter.build()

      streamContext.flow.append(statusCodeFilter).withSink(fLinkSender).run()

      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/1")
      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/status/301")
      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/status/404")
      streamContext.api ! WrappedHttpRequest.getUrl("http://localhost:9090/status/503")

      probes(1).receiveN(2).toSet should === (Set("http://localhost:9090/1", 
        "http://localhost:9090/status/301"))
      probes(1).expectNoMsg()
      streamContext.shutdown()
    }

  }

}