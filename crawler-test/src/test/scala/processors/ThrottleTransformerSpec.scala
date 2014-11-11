package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import org.blikk.crawler.processors.{ThrottleTransformer}
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._

class ThrottleTransformerSpec extends AkkaSingleNodeSpec("ThrottleTransformerSpec") {

  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  describe("hrottleTransformer") {
    
    it("should work aggregate items"){
      val data = Iterator from 1
      val senderSink = Sink.foreach[Int] { self ! _ }
      val flow = Source(data)
        .timerTransform("throttler", () => new ThrottleTransformer[Int](250.millis))
        .to(senderSink)
        .run()

      expectMsg(1)
      expectNoMsg(200.millis)
      expectMsg(2)
      expectNoMsg(200.millis)
      expectMsg(3)
      expectNoMsg(200.millis)
      expectMsg(4)
    }

  }

}