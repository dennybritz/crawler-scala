package org.blikk.test


import akka.actor._
import scala.collection.mutable.ArrayBuffer
import org.scalatest.{FunSpec, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit._
import com.typesafe.config.ConfigFactory


class AkkaRemoteSpec(name: String) extends FunSpec with BeforeAndAfter 
  with BeforeAndAfterAll with LocalRabbitMQ {

  var systems = ArrayBuffer[ActorSystem]()
  var probes = ArrayBuffer[TestProbe]()

  def addNode(name: String, port: Int) : Unit = {
    val config = ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port = ${port}
      akka.remote.netty.tcp.hostname = localhost
      akka.actor.provider = akka.remote.RemoteActorRefProvider
    """).withFallback(ConfigFactory.load("application.test"))
    val newSystem = ActorSystem(name, config)
    val newProbe = new TestProbe(newSystem)
    systems += newSystem
    probes += newProbe
  }

  def shutdown(){
    systems.foreach(_.shutdown())
  }

} 
