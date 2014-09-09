package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.redis.RedisClientPool

class SimpleCrawlerSpecMultiJvmNode1 extends SimpleCrawlerSpec
class SimpleCrawlerSpecMultiJvmNode2 extends SimpleCrawlerSpec
class SimpleCrawlerSpecMultiJvmNode3 extends SimpleCrawlerSpec

class SimpleCrawlerSpec extends CrawlClusterSpec {
  
  import CrawlClusterConfig._

  describe("A crawl cluster doing localhost requests"){
    
    it("should work"){      

      // Find the absolute actor address
      val baseAddress = system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress.toString
      val remoteActorAddress = baseAddress + self.path.toStringWithoutAddress
      log.info(remoteActorAddress)

      // The job configuration
      val simpleJobConf = JobConfiguration.empty("testJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/1"), WrappedHttpRequest.getUrl("http://localhost:9090/2")),
        processors = List(new RemoteTestResponseProcessor(remoteActorAddress))
      );

      startHttpServer()

      // Start all nodes in the cluster
      Cluster(system).subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp])
      Cluster(system) join node(node1).address
      val localRedis = new RedisClientPool("localhost", 6379)
      system.actorOf(CrawlService.props(localRedis), name = s"crawlService")
      roles.foreach { r => expectMsgClass(classOf[MemberUp]) }
      Cluster(system).unsubscribe(self)
      testConductor.enter("cluster-up")

      // Send the job to node 1 and wait for a response
      runOn(node1) {
        val service = system.actorSelection("akka://" + system.name + "/user/crawlService")
        service ! RunJob(simpleJobConf)
        receiveN(2).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2")
        expectNoMsg()
      }
      testConductor.enter("job-run-1")

      // Send another request on each node 
      // It should be routed appropriately
      val fetchReq = FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob")
      val service = system.actorSelection("akka://" + system.name + "/user/crawlService")
      service ! RouteFetchRequest(fetchReq)
      // We should receive 3 responses on node1
      runOn(node1) {
        expectMsg("http://localhost:9090/3")
        expectNoMsg()
      }
      testConductor.enter("finished")

    }

  }

}
