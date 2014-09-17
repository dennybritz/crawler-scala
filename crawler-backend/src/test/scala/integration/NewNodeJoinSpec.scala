package org.blikk.test

import org.blikk.crawler._
import scala.concurrent.duration._

class NewNodeJoinIntegrationSpec extends IntegrationSuite("NewNodeJoinIntegrationSpec") {

  describe("When a new node joins the cluster") {
    
    it("should be able to do work") {

      services.foreach(_ ! TestCrawlService.SetTarget(probes(1).ref))
      services.foreach(_ ! ClearFrontier)
      services(0) ! AddToFrontier(FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/1"), "testJob"))
      probes(1).expectMsg("http://localhost:9090/1")
      probes.foreach(_.expectNoMsg())

      addNode(s"${name}-4", "localhost", 8083)
      services(3) ! TestCrawlService.SetTarget(probes(1).ref)
      services(3) ! AddToFrontier(FetchRequest(
        WrappedHttpRequest.getUrl("http://localhost:9090/2"), "job"))
      probes(1).expectMsg("http://localhost:9090/2")

    }

  }


}