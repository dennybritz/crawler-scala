package org.blikk.test

import akka.stream.scaladsl2._
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.testkit._

class GroupThrottlerSpec extends AkkaSingleNodeSpec("GroupThrottlerSpec") {
  
  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  describe("The Group Throttler") {

    it("should work with a single key") {
      val data = Iterator from 1
      val throttler = new GroupThrottler[Int](300.millis)( x => "staticKey")
      val senderTrans = FlowFrom[Int].map { self ! _ }

      FlowFrom(data)
        .timerTransform("throttle", () => throttler)
        .append(senderTrans)
        .take(3)
        .withSink(BlackholeSink)
        .run()

      expectMsg(1)
      expectNoMsg(200.millis)
      expectMsg(2)
      expectNoMsg(200.millis)
      expectMsg(3)
      expectNoMsg(200.millis)
    }

    it("should work with multiple keys") {
      val data = Iterator from 1
      val throttler = new GroupThrottler[Int](300.millis)( x => (x % 2).toString)
      val senderTrans = FlowFrom[Int].map { self ! _ }

      FlowFrom(data)
        .timerTransform("throttle", () => throttler)
        .append(senderTrans)
        .take(6)
        .withSink(BlackholeSink)
        .run()

      receiveN(2).toSet shouldEqual (Set(1,2))
      expectNoMsg(200.millis)
      receiveN(2).toSet shouldEqual (Set(3,4))
      expectNoMsg(200.millis)
      receiveN(2).toSet shouldEqual (Set(5,6))
      expectNoMsg(200.millis)
    }

  }

}