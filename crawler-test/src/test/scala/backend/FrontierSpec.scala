package org.blikk.test

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit._
import akka.testkit.TestActorRef
import org.blikk.crawler._
import org.blikk.crawler.processors.FrontierSink
import scala.concurrent.duration._

class FrontierSpec extends AkkaSingleNodeSpec("FrontierSpec") {
    
  implicit val fmat = FlowMaterializer(MaterializerSettings(system))

  describe("The Frontier") {

    it("should work with immediate requests") {
      val probe = TestProbe()
      val frontier = TestActorRef(Frontier.props(probe.ref))

      val frontierSink = FrontierSink.build("testJob")
      val req1 = WrappedHttpRequest.getUrl("http://localhost:9090/1")
      val req2 = WrappedHttpRequest.getUrl("http://localhost:9090/2")
      Source(List(req1, req2)).runWith(frontierSink)

      probe.receiveN(2).map(_.asInstanceOf[FetchRequest].req.uri.toString).toSet shouldEqual Set(
        "http://localhost:9090/1", "http://localhost:9090/2")
      frontier.stop()
    }

    // it("should work with scheduled requests") {
    //   val frontier = TestActorRef(Frontier.props(self))
    //   val req1 = WrappedHttpRequest.getUrl("http://localhost:9090/1")
    //   val scheduledTime = System.currentTimeMillis + 3*1000 // + 4 seconds
    //   val req2 = WrappedHttpRequest.getUrl("http://localhost:9090/2")
    //   frontier.receive(AddToFrontier(FetchRequest(req1, "testJob")))
    //   frontier.receive(AddToFrontier(FetchRequest(req2, "testJob"), Option(scheduledTime)))
      
    //   val res1 = expectMsgClass(classOf[FetchRequest])
    //   res1.req.uri shouldEqual req1.uri
    //   expectNoMsg(1.seconds)
      
    //   val res2 = expectMsgClass(5.seconds, classOf[FetchRequest])
    //   res2.req.uri shouldEqual req2.uri

    //   frontier.stop()
    // }

  }

}