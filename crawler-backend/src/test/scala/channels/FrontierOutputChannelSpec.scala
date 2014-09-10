package org.blikk.test

import akka.actor.{ActorRef, ActorSystem}
import org.blikk.crawler.channels.{FrontierOutputChannel, FrontierChannelInput}
import org.blikk.crawler.channels.FrontierChannelInput.AddToFrontierRequest
import org.blikk.crawler._

class FrontierOutputChannelSpec extends AkkaSingleNodeSpec("FrontierOutputChannelSpec") {

  describe("FrontierOutputChannel") {

    it("should send a RouteFetchRequest message for each URL") {
      val newRequests = Seq(
        WrappedHttpRequest.getUrl("http://google.com"), 
        WrappedHttpRequest.getUrl("http://cnn.com"),
        WrappedHttpRequest.getUrl("http://localhost:9090"))
      val input = new FrontierChannelInput(newRequests.map(r => AddToFrontierRequest(r)))
      val foc = new FrontierOutputChannel(self)
      foc.pipe(input, JobConfiguration.empty("testJob"), Map.empty)

      newRequests.foreach { req =>
         expectMsg(RouteFetchRequest(AddToFrontier(req, "testJob")))
      }
    }

  }

}
