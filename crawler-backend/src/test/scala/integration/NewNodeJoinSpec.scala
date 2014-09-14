package org.blikk.test

import org.blikk.crawler._
import org.blikk.crawler.processors._
import scala.concurrent.duration._

class NewNodeJoinIntegrationSpec extends IntegrationSuite("NewNodeJoinIntegrationSpec") {

  describe("When a new node joins the cluster") {
    
    it("should be able to do work") {

      // Submit a new job to the existing clister
      val processors = List(
        new LinkExtractor("link-extractor"),
        new TestResponseProcessor(probes(1).ref)(systems(1)))
      val jobConf = JobConfiguration.empty("job").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/1")),
        processors = processors
      )
      services(0) ! RunJob(jobConf, true)
      probes(1).expectMsg("http://localhost:9090/1")
      probes.foreach(_.expectNoMsg())


      addNode(s"${name}-4")
      services(3) ! AddToFrontier(
        WrappedHttpRequest.getUrl("http://localhost:9090/2"), "job")
      probes(1).expectMsg("http://localhost:9090/2")

    }

  }


}