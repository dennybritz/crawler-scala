package org.blikk.test

import com.redis.RedisClient
import org.blikk.crawler.processors.{LinkExtractor, RabbitMQProducer}
import org.blikk.crawler._
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import scala.concurrent.duration._

class StandardCrawlSpecMultiJvmNode1 extends StandardCrawlSpec
class StandardCrawlSpecMultiJvmNode2 extends StandardCrawlSpec
class StandardCrawlSpecMultiJvmNode3 extends StandardCrawlSpec

class StandardCrawlSpec extends CrawlClusterSpec {
  
  import CrawlClusterConfig._

  // TODO: Put this into a configuration setting
  val rabbitMQconnectionString = "amqp://guest:guest@localhost:5672"
  val processors = List(
    new LinkExtractor("linkExtractor", None),
    new RabbitMQProducer("testRabbitMQProducer", rabbitMQconnectionString, "org.blikk.test.StandardCrawlSpec")
  )

  describe("A crawl cluster doing localhost requests"){
    
    it("should work"){

      // Find the absolute actor address
      val baseAddress = system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress.toString
      val remoteActorAddress = baseAddress + self.path.toStringWithoutAddress
      log.info(remoteActorAddress)

      // The job configuration
      val jobConf = JobConfiguration.empty("testJob").copy(
        seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/links/1")),
        processors = processors ++ List(new RemoteTestResponseProcessor(remoteActorAddress))
      );

      startHttpServer()

      // Start all nodes in the cluster and wait for them to join
      Cluster(system).subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp])
      Cluster(system) join node(node1).address
      val localRedis = new RedisClient("localhost", 6379)
      system.actorOf(CrawlService.props(localRedis), name = s"crawlService")
      roles.foreach { r => expectMsgClass(classOf[MemberUp]) }
      Cluster(system).unsubscribe(self)
      testConductor.enter("cluster-up")

      // Send the job to node 1 and wait for a response
      runOn(node1) {
        val service = system.actorSelection("akka://" + system.name + "/user/crawlService")
        service ! RunJob(jobConf)
        receiveN(3, 10.seconds).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2", "http://localhost:9090/3")
      }
      testConductor.enter("job-run-1")

      expectNoMsg()

    }

  }

}
