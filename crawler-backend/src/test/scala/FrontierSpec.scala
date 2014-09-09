package org.blikk.test

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.redis.RedisClientPool
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.testkit.TestActorRef


class FrontierSpec extends AkkaSingleNodeSpec("FrontierBehaviorSpec") with LocalRedis {

  describe("The Frontier") {

    it("should work with immediate requests") {
      val frontier = TestActorRef(Frontier.props("testJob", localRedis))
      frontier.receive(ClearFrontier)
      frontier.receive(StartFrontier(500.millis, self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2")
      frontier.receive(AddToFrontier(req1, "testJob"))
      frontier.receive(AddToFrontier(req2, "testJob"))
      receiveN(2).toSet == Set(FetchRequest(WrappedHttpRequest.getUrl("localhost:9090/1"), "testJob"),
        FetchRequest(WrappedHttpRequest.getUrl("localhost:9090/2"), "testJob"))
      frontier.stop()
    }

    it("should work with scheduled requests") {
      val frontier = TestActorRef(Frontier.props("testJob", localRedis))
      frontier.receive(ClearFrontier)
      frontier.receive(StartFrontier(500.millis, self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val scheduledTime = System.currentTimeMillis + 3*1000 // + 4 seconds
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2").copy(scheduledTime=Option(scheduledTime))
      frontier.receive(AddToFrontier(req1, "testJob"))
      frontier.receive(AddToFrontier(req2, "testJob"))
      expectMsg(FetchRequest(req1, "testJob"))
      expectNoMsg(1.seconds)
      expectMsg(5.seconds, FetchRequest(req2, "testJob"))
      frontier.stop()
    }

  }

}