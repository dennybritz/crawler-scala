package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.testkit._

class GroupThrottlerSpec extends AkkaSingleNodeSpec("GroupThrottlerSpec") {
  
  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  describe("The Group Throttler") {

    it("should work with a single key") {
      val probe = TestProbe()
      val data = Iterator from 1
      val throttler = new GroupThrottler[Int](300.millis)( x => "staticKey")
      val senderTrans = Flow[Int].map { probe.ref ! _ }

      Source(data)
        .timerTransform("throttle", () => throttler)
        .via(senderTrans)
        .take(3)
        .to(Sink.ignore)
        .run()

      probe.expectMsg(1)
      probe.expectNoMsg(200.millis)
      probe.expectMsg(2)
      probe.expectNoMsg(200.millis)
      probe.expectMsg(3)
      probe.expectNoMsg(200.millis)
    }

    it("should work with multiple keys") {
      val probe = TestProbe()
      val data = Iterator from 1
      val throttler = new GroupThrottler[Int](300.millis)( x => (x % 2).toString)
      val senderTrans = Flow[Int].map { probe.ref ! _ }

      Source(data)
        .timerTransform("throttle", () => throttler)
        .via(senderTrans)
        .take(6)
        .to(Sink.ignore)
        .run()

      probe.receiveN(2).toSet shouldEqual (Set(1,2))
      probe.expectNoMsg(200.millis)
      probe.receiveN(2).toSet shouldEqual (Set(3,4))
      probe.expectNoMsg(200.millis)
      probe.receiveN(2).toSet shouldEqual (Set(5,6))
      probe.expectNoMsg(200.millis)
    }

    it("should work with custom intervals") {
      val probe = TestProbe()
      val data = Iterator from 1
      val throttler = new GroupThrottler[Int](100.millis,
        Map("staticKeyCustomInterval" -> 300.millis))( x => "staticKeyCustomInterval")
      val senderTrans = Flow[Int].map { probe.ref ! _ }

      Source(data)
        .timerTransform("throttle", () => throttler)
        .via(senderTrans)
        .take(3)
        .to(Sink.ignore)
        .run()

      probe.expectMsg(1)
      probe.expectNoMsg(200.millis)
      probe.expectMsg(2)
      probe.expectNoMsg(200.millis)
      probe.expectMsg(3)
      probe.expectNoMsg(200.millis)
    }


  }

}