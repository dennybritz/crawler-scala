package org.blikk.test

import akka.actor._
import akka.routing.{AddRoutee, ActorRefRoutee}
import scala.collection.mutable.ArrayBuffer
import org.scalatest.{FunSpec, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit._
import com.typesafe.config.ConfigFactory

class IntegrationSuite(name: String) extends FunSpec with BeforeAndAfter with BeforeAndAfterAll 
  with LocalRedis with LocalRabbitMQ {

  var systems = ArrayBuffer[ActorSystem]()
  var services = ArrayBuffer[ActorRef]()
  var probes = ArrayBuffer[TestProbe]()

  before {
    // Start a crawl service on each system
    services += systems(0).actorOf(TestCrawlService.props(localRedis, "test-1:"), "crawlService-1")
    services += systems(1).actorOf(TestCrawlService.props(localRedis, "test-2:"), "crawlService-2")
    services += systems(2).actorOf(TestCrawlService.props(localRedis, "test-3:"), "crawlService-3")
    // We add the other systems to the router
    // In a real cluster this happens automatically
    services(0) ! AddRoutee(ActorRefRoutee(services(1)))
    services(0) ! AddRoutee(ActorRefRoutee(services(2)))
    services(1) ! AddRoutee(ActorRefRoutee(services(0)))
    services(1) ! AddRoutee(ActorRefRoutee(services(2)))
    services(2) ! AddRoutee(ActorRefRoutee(services(0)))
    services(2) ! AddRoutee(ActorRefRoutee(services(1)))
    // We watch all crawl services
    probes += new TestProbe(systems(0))
    probes += new TestProbe(systems(1))
    probes += new TestProbe(systems(2))
    probes.zipWithIndex.foreach { case (probe, num) =>
      probe.watch(services(num))
    }
  }

  after {
    // Stop the crawl service on each system
    systems.zipWithIndex.foreach { case (system, num) =>
      system.stop(services(num))
      probes(num).expectMsgClass(classOf[Terminated])
      probes(num).unwatch(services(num))
    }
  }

  override def beforeAll() {
    // Start all actor systems
    systems += ActorSystem(s"${name}-1", ConfigFactory.load("application.test"))
    systems += ActorSystem(s"${name}-2", ConfigFactory.load("application.test"))
    systems += ActorSystem(s"${name}-3", ConfigFactory.load("application.test"))
    // Start a local HTTP server
    TestHttpServer.start("localhost", 9090)(systems(1))
  }

  override def afterAll() {
    // Shutdown all actor systems
    systems(0).shutdown()
    systems(1).shutdown()
    systems(2).shutdown()
  }

}