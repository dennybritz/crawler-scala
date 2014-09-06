package org.blikk.test

import akka.actor.ActorSystem
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit.{TestKit, ImplicitSender, TestActorRef}
import com.typesafe.config.ConfigFactory

class AkkaSingleNodeSpec(name: String) extends TestKit(ActorSystem(name, ConfigFactory.load("application.test"))) 
  with ImplicitSender with FunSpecLike with BeforeAndAfter with BeforeAndAfterAll {

  override def beforeAll() {
    // Start a local HTTP server for request testing
    TestHttpServer.start("localhost", 9090)
  }

  override def afterAll { 
    TestKit.shutdownActorSystem(system)
  }

}