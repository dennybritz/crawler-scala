package org.blikk.test

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.redis.RedisClientPool
import org.blikk.crawler._
import scala.concurrent.duration._
import akka.testkit.TestActorRef


class FrontierSpec extends AkkaSingleNodeSpec("FrontierBehaviorSpec") 
  with LocalRabbitMQ {

  describe("The Frontier") {

    it("should work with immediate requests") {
      val frontier = TestActorRef(Frontier.props(factory.newConnection(), self))
      frontier.receive(ClearFrontier)
      frontier.receive(StartFrontier(500.millis, self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob")))
      receiveN(2).toSet == Set(
        RouteFetchRequest(FetchRequest(WrappedHttpRequest.getUrl("localhost:9090/1"), "testJob")),
        RouteFetchRequest(FetchRequest(WrappedHttpRequest.getUrl("localhost:9090/2"), "testJob")))
      frontier.stop()
    }

    it("should work with scheduled requests") {
      val frontier = TestActorRef(Frontier.props(factory.newConnection(), self))
      frontier.receive(ClearFrontier)
      frontier.receive(StartFrontier(500.millis, self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val scheduledTime = System.currentTimeMillis + 3*1000 // + 4 seconds
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob"), Option(scheduledTime)))
      expectMsg(RouteFetchRequest(FetchRequest(req1, "testJob")))
      expectNoMsg(1.seconds)
      expectMsg(5.seconds, RouteFetchRequest(FetchRequest(req2, "testJob")))
      frontier.stop()
    }

  }

}