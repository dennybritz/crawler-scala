package org.blikk.test

import akka.actor.ActorSystem
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll, Matchers}
import akka.testkit.{TestKit, ImplicitSender, TestActorRef}
import org.blikk.crawler.RabbitData

class AkkaSingleNodeSpec(val name: String) extends TestKit(ActorSystem(name, TestConfig.config))
  with LocalRabbitMQ with ImplicitSender with FunSpecLike 
  with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  lazy val log = akka.event.Logging.getLogger(system, this)

  override def beforeAll() {
    withLocalRabbit { implicit c => RabbitData.declareAll() }
    clearRabbitMQ()
    TestHttpServer.start()
  }

  override def afterAll { 
    TestKit.shutdownActorSystem(system)
    clearRabbitMQ()
  }

}