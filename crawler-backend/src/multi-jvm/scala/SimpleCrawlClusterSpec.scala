package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.cluster.Cluster
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender


class SimpleCrawlerSpecMultiJvmNode1 extends SimpleCrawlerSpec
class SimpleCrawlerSpecMultiJvmNode2 extends SimpleCrawlerSpec
class SimpleCrawlerSpecMultiJvmNode3 extends SimpleCrawlerSpec


class SimpleCrawlerSpec extends CrawlClusterSpec {
  
  import CrawlClusterConfig._

  val testProcessor = new TestResponseProcessor(self)

  describe("A crawl cluster doing localhost requests"){
    
    it("should work"){      
      
      startHttpServer()

      val simpleJobConf = JobConfiguration.empty("testJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090"), WrappedHttpRequest.getUrl("http://localhost:9090")),
        processors = List(new TestResponseProcessor(self))
      );

      // Start all nodes in the cluster
      roles.foreach { nodeRole =>
        runOn(nodeRole) {
          Cluster(system) join node(node1).address
          val service = system.actorOf(Props[CrawlService], name = s"crawlService")
        }
      }
      testConductor.enter("cluster-started")

      // Send the job to node 1
      runOn(node1) {
        val service = system.actorSelection("akka://" + system.name + "/user/crawlService")
        service ! RunJob(simpleJobConf)
        expectMsg("success!")
        expectMsg("success!")
      }

    }

  }

}
