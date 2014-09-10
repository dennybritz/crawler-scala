package org.blikk.test

import akka.actor.{ActorRef, ActorSystem}
import org.blikk.crawler.channels.{FrontierOutputChannel, FrontierChannelInput}
import org.blikk.crawler._

// Sends the message to the target instead of a predefined path for testing
class TestFOC(target: ActorRef)(implicit system: ActorSystem) extends FrontierOutputChannel {
  override def serviceActor = system.actorSelection(target.path) 
}

class FrontierOutputChannelSpec extends AkkaSingleNodeSpec("FrontierOutputChannelSpec") {

  describe("FrontierOutputChannel") {

    it("should send a RouteFetchRequest message for each URL") {
      val newRequests = Seq(
        WrappedHttpRequest.getUrl("http://google.com"), 
        WrappedHttpRequest.getUrl("http://cnn.com"),
        WrappedHttpRequest.getUrl("http://localhost:9090"))
      val input = new FrontierChannelInput(newRequests)
      val foc = new TestFOC(self)
      foc.pipe(input, JobConfiguration.empty("testJob"), Map.empty)

      newRequests.foreach { req =>
         expectMsg(RouteFetchRequest(FetchRequest(req, "testJob")))
      }
    }

  }

}
