package org.blikk.test

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.testkit.TestActorRef
import org.blikk.crawler._
import scala.concurrent.duration._

class FrontierSpec extends AkkaSingleNodeSpec("FrontierSpec") {
  
  describe("The Frontier") {

    it("should work with immediate requests") {
      val frontier = TestActorRef(Frontier.props(self))
      val req1 = WrappedHttpRequest.getUrl("http://localhost:9090/1")
      val req2 = WrappedHttpRequest.getUrl("http://localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob")))

      receiveN(2).toSet == Set(
        FetchRequest(req1, "testJob"),
        FetchRequest(req2, "testJob"))
      frontier.stop()
    }

    it("should work with scheduled requests") {
      val frontier = TestActorRef(Frontier.props(self))
      val req1 = WrappedHttpRequest.getUrl("http://localhost:9090/1")
      val scheduledTime = System.currentTimeMillis + 3*1000 // + 4 seconds
      val req2 = WrappedHttpRequest.getUrl("http://localhost:9090/2")
      frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
      frontier.receive(AddToFrontier(FetchRequest(req2, "testJob"), Option(scheduledTime)))
      
      val res1 = expectMsgClass(classOf[FetchRequest])
      res1.req.uri shouldEqual req1.uri
      expectNoMsg(1.seconds)
      
      val res2 = expectMsgClass(5.seconds, classOf[FetchRequest])
      res2.req.uri shouldEqual req2.uri

      frontier.stop()
    }

  }

}