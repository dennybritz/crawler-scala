package org.blikk.test

import akka.actor._
import akka.testkit._
import org.blikk.crawler._

class ApiLayerSpec extends AkkaSingleNodeSpec("ApiLayerSpec") {

  def apiProps(serviceActor: ActorRef) = Props(classOf[ApiLayer], serviceActor)

  describe("ApiLayer") {

    it("should be able to route fetch requests"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      val httpRequest = WrappedHttpRequest.getUrl("localhost")
      api ! ApiRequest(FetchRequest(httpRequest, "testJob"))
      probe.expectMsg(AddToFrontier(FetchRequest(httpRequest, "testJob")))
      expectMsg(ApiResponse.OK)
      api.stop()
    }


    it("should return an error for unhandled messages"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      api ! ApiRequest("other message")
      expectMsgClass(classOf[ApiError])
      api.stop()
    }

    it("should return an error for messages not wrapped in an ApiRequest"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      api ! "other message"
      expectMsgClass(classOf[ApiError])
      api.stop()
    }

  }

}