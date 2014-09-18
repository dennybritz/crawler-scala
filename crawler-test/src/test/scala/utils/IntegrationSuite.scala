package org.blikk.test

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.routing.{AddRoutee, ActorRefRoutee}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.scalatest.{FunSpec, BeforeAndAfter, BeforeAndAfterAll, Matchers}
import scala.collection.mutable.ArrayBuffer

class IntegrationSuite(val name: String) extends FunSpec with BeforeAndAfter with BeforeAndAfterAll 
  with Matchers with LocalRabbitMQ {

  lazy val log = akka.event.Logging.getLogger(systems(0), this)
  val appId = name

  val httpSystem = ActorSystem("httpServer", TestConfig.config)
  var systems = ArrayBuffer[ActorSystem]()
  var services = ArrayBuffer[ActorRef]()
  var apis = ArrayBuffer[ActorRef]()
  var probes = ArrayBuffer[TestProbe]()
  var addresses = ArrayBuffer[String]()

  override def beforeAll(){
    clearRabbitMQ()
    deleteQueue(appId)
    TestHttpServer.start()(httpSystem)
    addNode(name, "localhost", 13371)
    addNode(name, "localhost", 13372)
    addNode(name, "localhost", 13373)
    probes.zipWithIndex.foreach { case (p, num) =>
      p.awaitCond({Cluster.get(systems(num)).state.members.size == 3})
    }
  }

  override def afterAll(){
    httpSystem.shutdown()
    httpSystem.awaitTermination()
    systems.zipWithIndex.foreach { case (system, num) =>
      system.stop(services(num))
      system.shutdown()
      system.awaitTermination()
    }
    clearRabbitMQ()
    deleteQueue(appId)
  }

  /* Add a new node to the cluster */
  def addNode(name: String, host: String, port: Int) : Unit = {
    val newAddress = s"akka.tcp://${name}@${host}:${port}"
    addresses += newAddress
    val newSystem = ActorSystem(name, buildConfig(host, port))
    systems += newSystem
    val cluster = Cluster.get(newSystem)
    cluster.joinSeedNodes(List(AddressFromURIString.parse(
      addresses.headOption.getOrElse(newAddress))))
    val newService = newSystem.actorOf(
      CrawlService.props(factory.newConnection()), s"crawlService")
    services += newService
    val newApi = newSystem.actorOf(ApiLayer.props(newService), "api")
    apis += newApi
    val newProbe = new TestProbe(newSystem)
    probes += newProbe
  }

  /* Runs the program */
  def createStreamContext() = {
    val config = ConfigFactory.parseString(s"""
      akka.actor.provider = akka.remote.RemoteActorRefProvider
      """).withFallback(buildConfig("localhost", 0))
    val system = ActorSystem(appId, config)
    val client = new CrawlerApp(addresses(0) + "/user/api", appId)(system)
    client.start[CrawlItem]()
  }

  /* Builds a configuration for a new actor system. */
  private def buildConfig(host: String, port: Int) = {
    ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port = ${port}
      akka.remote.netty.tcp.hostname = ${host}
      akka.actor.provider = akka.cluster.ClusterActorRefProvider
    """).withFallback(TestConfig.config)
  }

}