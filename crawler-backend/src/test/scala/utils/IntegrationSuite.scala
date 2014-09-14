package org.blikk.test

import akka.actor._
import akka.routing.{AddRoutee, ActorRefRoutee}
import scala.collection.mutable.ArrayBuffer
import org.scalatest.{FunSpec, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit._
import com.typesafe.config.ConfigFactory

class IntegrationSuite(val name: String) extends FunSpec with BeforeAndAfter with BeforeAndAfterAll 
  with LocalRedis with LocalRabbitMQ {

  var systems = ArrayBuffer[ActorSystem]()
  var services = ArrayBuffer[ActorRef]()
  var probes = ArrayBuffer[TestProbe]()

  before {
    addNode(s"${name}-1")
    addNode(s"${name}-2")
    TestHttpServer.start("localhost", 9090)(systems(1))
    addNode(s"${name}-3")
  }

  after {
    // Stop the crawl service on each system
    systems.zipWithIndex.foreach { case (system, num) =>
      system.stop(services(num))
      probes(num).expectMsgClass(classOf[Terminated])
      probes(num).unwatch(services(num))
      system.shutdown()
      system.awaitTermination()
    }
  }

  // Adds a new node
  def addNode(name: String) : Unit = {
    val newSystem = ActorSystem(name, ConfigFactory.load("application.test"))
    val newService = newSystem.actorOf(
      TestCrawlService.props(localRedis, s"${name}:"), s"crawlService-${name}")
    val newProbe = new TestProbe(newSystem)
    newProbe.watch(newService)
    services.foreach { existingService =>
      existingService ! AddRoutee(ActorRefRoutee(newService)) 
      newService ! AddRoutee(ActorRefRoutee(existingService)) 
    }
    systems += newSystem
    services += newService
    probes += newProbe
  }


}