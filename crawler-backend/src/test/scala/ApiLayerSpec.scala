package org.blikk.test

import akka.actor._
import akka.testkit._
import org.blikk.crawler._

class ApiLayerSpec extends AkkaSingleNodeSpec("ApiLayerSpec") {

  def apiProps(serviceActor: ActorRef) = Props(classOf[ApiLayer], serviceActor)

  describe("ApiLayer") {

    it("should be able to run jobs"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      val jobConf = JobConfiguration.empty("testJob")
      api ! ApiRequest(RunJob(jobConf))
      probe.expectMsg(RunJob(jobConf))
      expectMsg(ApiResponse("ok"))
      api.stop()
    }

    it("should be able to stop jobs"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      api ! ApiRequest(StopJob("testJob"))
      probe.expectMsg(StopJob("testJob"))
      expectMsg(ApiResponse("ok"))
      api.stop()
    }

    it("should be able to destroy jobs"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      api ! ApiRequest(DestroyJob("testJob"))
      probe.expectMsg(DestroyJob("testJob"))
      expectMsg(ApiResponse("ok"))
      api.stop()
    }

    it("should be able to get job statistics"){
      val probe = TestProbe()
      val api = TestActorRef(apiProps(probe.ref))
      api ! ApiRequest(GetJobEventCounts("testJob"))
      probe.expectMsg(GetJobEventCounts("testJob"))
      probe.reply(Map("someStats" -> "yay"))
      expectMsg(ApiResponse(Map("someStats" -> "yay")))
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