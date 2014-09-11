package org.blikk.test

import akka.actor.ActorSystem
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit.{TestKit, ImplicitSender, TestActorRef}

class AkkaSingleNodeSpec(name: String) extends TestKit(ActorSystem(name, TestUtils.testConfig)) 
  with ImplicitSender with FunSpecLike with BeforeAndAfter with BeforeAndAfterAll with LocalRedis {

  override def beforeAll() {
    // Start a local HTTP server for request testing
    TestHttpServer.start("localhost", 9090)
  }

  override def afterAll { 
    TestKit.shutdownActorSystem(system)
  }

}