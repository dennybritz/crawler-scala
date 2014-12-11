package org.blikk.test

import akka.actor._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.testkit._

class RabbitThrottlerSpec extends AkkaSingleNodeSpec("RabbitThrottlerSpec") {

  val TestRoutingKey = "some.routing.key"
  val TestExchange = RabbitExchangeDefinition("com.blikk.test.rabbit-throttler-spec-ex", "topic", false, false)
  implicit val rabbitChannel = RabbitData.createChannel()

  class TestRabbitThrottler(target: ActorRef) extends RabbitThrottler {
    def rabbitExchange = TestExchange
    def bindRoutingKey = "#"
    def handleItem(item: RabbitMessage) {
      target ! new String(item.payload)
    }
  }

  describe("RabbitThrottler") {

    before {
      RabbitData.declareExchange(TestExchange)
    }

    after {
      RabbitData.deleteExchange(TestExchange)
    }

    it("should work with one schedule") {
      val probe = TestProbe()
      val throttler = system.actorOf(Props(new TestRabbitThrottler(probe.ref)))
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      // Wait for the throttler to startup before we publish messages
      expectMsgClass(classOf[String])
      Thread.sleep(100)
      publishMsg("hello".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("world".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("SENDMELATER".getBytes, TestExchange.name, TestRoutingKey)
      probe.expectMsg("hello")
      probe.expectNoMsg(150.millis)
      probe.expectMsg("world")
      throttler ! RabbitThrottler.RemoveSchedule(TestRoutingKey)
      probe.expectNoMsg(400.millis)
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      probe.expectMsg("SENDMELATER")
      system.stop(throttler)
    }

    it("should be indempotent to adding the same schedule") {
      val probe = TestProbe()
      val throttler = system.actorOf(Props(new TestRabbitThrottler(probe.ref)))
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      expectMsgClass(classOf[String])
      Thread.sleep(100)
      publishMsg("hello".getBytes, TestExchange.name, TestRoutingKey)
      probe.expectMsg("hello")
      probe.expectNoMsg(400.millis)
      system.stop(throttler)
    }

    it("should work with multiple schedules") {
      val probe = TestProbe()
      val throttler = system.actorOf(Props(new TestRabbitThrottler(probe.ref)))
      throttler ! RabbitThrottler.AddSchedule(TestRoutingKey, 0.millis, 200.millis)
      expectMsgClass(classOf[String])
      throttler ! RabbitThrottler.AddSchedule("anotherRoutingKey", 0.millis, 500.millis)
      expectMsgClass(classOf[String])
      Thread.sleep(100)

      publishMsg("1".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("2".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("3".getBytes, TestExchange.name, "anotherRoutingKey")
      publishMsg("4".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("5".getBytes, TestExchange.name, TestRoutingKey)
      publishMsg("6".getBytes, TestExchange.name, "anotherRoutingKey")
      publishMsg("7".getBytes, TestExchange.name, "anotherRoutingKey")

      probe.expectMsg("1")
      probe.expectMsg("2")
      probe.expectMsg("3")
      probe.expectMsg("4")
      probe.expectMsg("5")
      probe.expectMsg("6")
      probe.expectNoMsg(400.millis)
      probe.expectMsg("7")

      probe.expectNoMsg(500.millis)
      system.stop(throttler)
    }

  }

}