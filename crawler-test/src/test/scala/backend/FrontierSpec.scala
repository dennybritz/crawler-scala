package org.blikk.test

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.testkit.TestActorRef
import org.blikk.crawler._
import scala.concurrent.duration._

class FrontierSpec extends AkkaSingleNodeSpec("FrontierSpec") {
  
  describe("The Frontier") {

    it("should work with immediate requests") {
      val frontier = TestActorRef(Frontier.props(rabbitFactory.newConnection(), self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob")))

      receiveN(2).toSet == Set(
        ConsistentHashableEnvelope(FetchRequest(req1, "testJob"), req1.host),
        ConsistentHashableEnvelope(FetchRequest(req2, "testJob"), req2.host))
      frontier.stop()
    }

    it("should work with scheduled requests") {
      val frontier = TestActorRef(Frontier.props(rabbitFactory.newConnection(), self))
      val req1 = WrappedHttpRequest.getUrl("localhost:9090/1")
      val scheduledTime = System.currentTimeMillis + 3*1000 // + 4 seconds
      val req2 = WrappedHttpRequest.getUrl("localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob"), Option(scheduledTime)))
      expectMsg(ConsistentHashableEnvelope(FetchRequest(req1, "testJob"), req1.host))
      expectNoMsg(1.seconds)
      expectMsg(5.seconds, ConsistentHashableEnvelope(FetchRequest(req2, "testJob"), req2.host))
      frontier.stop()
    }

  }

}