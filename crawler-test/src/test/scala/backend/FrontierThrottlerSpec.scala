package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.actor._
import akka.testkit._
import org.blikk.crawler._
import org.blikk.crawler.processors._
import org.scalatest._
import scala.concurrent.duration._

class FrontierThrottlerSpec extends AkkaSingleNodeSpec("FrontierThrottlerSpec") {

  val appId = "FrontierThrottlerSpec"
  implicit val flowMaterializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  describe("FrontierThrottler") {
    
    before {
      deleteQueue("com.blikk.crawler.requests.google.com")
      deleteQueue("com.blikk.crawler.requests.twitter.com")
      deleteQueue("com.blikk.crawler.requests.facebook.com")
    }

    it("should work for one domain"){
      val probe = TestProbe()

      val requests = List(
        WrappedHttpRequest.getUrl(s"http://www.google.com"),
        WrappedHttpRequest.getUrl(s"http://www.google.com/2"),
        WrappedHttpRequest.getUrl(s"http://www.google.com/3")
      )

      // Start the actor and a schedule
      val frontierThrottler = system.actorOf(FrontierThrottler.props(100))

      probe.expectNoMsg(200.millis)

      Source(ActorPublisher(frontierThrottler)).runWith(Sink.foreach[FetchRequest](probe.ref ! _))
      Source(requests).runWith(FrontierSink.build(appId))

      probe.expectMsgClass(classOf[FetchRequest])
      probe.expectNoMsg(80.millis)
      probe.expectMsgClass(classOf[FetchRequest])
      probe.expectNoMsg(80.millis)
      probe.expectMsgClass(classOf[FetchRequest])
      probe.expectNoMsg()
      system.stop(frontierThrottler)
    }

    it("should time out amd restart the schedules automatically"){
      val probe = TestProbe()
      val frontierThrottler = system.actorOf(FrontierThrottler.props(100))

      // Schedule should be timed out after this
      probe.expectNoMsg(1500.millis)
      
      // Should re-create the schedule
      val requests = List(WrappedHttpRequest.getUrl(s"http://google.com"))
      Source(requests).runWith(FrontierSink.build(appId))
      Source(ActorPublisher(frontierThrottler)).runWith(Sink.foreach[FetchRequest](probe.ref ! _))
      probe.expectMsgClass(classOf[FetchRequest]).req.uri.toString shouldEqual "http://google.com"

      system.stop(frontierThrottler)
    }

    it("should work for multiple domains"){
      val probe = TestProbe()
      val frontierThrottler = system.actorOf(FrontierThrottler.props(100))
      probe.expectNoMsg(200.millis)

      val requests = List(
        WrappedHttpRequest.getUrl(s"http://www.google.com/"),
        WrappedHttpRequest.getUrl(s"http://www.twitter.com/"),
        WrappedHttpRequest.getUrl(s"http://www.facebok.com/"),
        WrappedHttpRequest.getUrl(s"http://www.google.com/2"),
        WrappedHttpRequest.getUrl(s"http://www.twitter.com/2"),
        WrappedHttpRequest.getUrl(s"http://www.facebok.com/2")
      )

      Source(ActorPublisher(frontierThrottler)).runWith(Sink.foreach[FetchRequest](probe.ref ! _))
      Source(requests).runWith(FrontierSink.build(appId))

      probe.receiveN(3).map(_.asInstanceOf[FetchRequest]).map(_.req.uri.toString).toSet shouldEqual 
        Set("http://www.google.com/", "http://www.twitter.com/", "http://www.facebok.com/")
      probe.expectNoMsg(50.millis)
      probe.receiveN(3).map(_.asInstanceOf[FetchRequest]).map(_.req.uri.toString).toSet shouldEqual 
        Set("http://www.google.com/2", "http://www.twitter.com/2", "http://www.facebok.com/2")

      probe.expectNoMsg()
      system.stop(frontierThrottler)
    }    

  }

}