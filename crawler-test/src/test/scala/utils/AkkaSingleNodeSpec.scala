package org.blikk.test

import akka.actor.ActorSystem
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll, Matchers}
import akka.testkit.{TestKit, ImplicitSender, TestActorRef}

class AkkaSingleNodeSpec(val name: String) extends TestKit(ActorSystem(name, TestConfig.config))
  with LocalRabbitMQ with ImplicitSender with FunSpecLike 
  with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  override def beforeAll() {
    clearRabbitMQ()
    TestHttpServer.start()
  }

  override def afterAll { 
    TestKit.shutdownActorSystem(system)
    clearRabbitMQ()
  }

}