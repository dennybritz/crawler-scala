package org.blikk.test

import org.blikk.crawler.app._
import org.blikk.crawler._
import akka.actor._
import akka.testkit._

class ApiClientSpec extends AkkaSingleNodeSpec("ApiClientSpec") {

  val probe = TestProbe()
  val endpoint = probe.ref.path.toString

  describe("API Client") {
    
    it("should be able to get connection information") {
      val actor = system.actorOf(ApiClient.props(endpoint, "testApp"))
      actor ! ConnectionInfoRequest
      probe.expectMsg(ApiRequest(ConnectionInfoRequest))
      probe.reply(ApiResponse(ConnectionInfo("someUri")))
      expectMsg(ConnectionInfo("someUri"))
      system.stop(actor)
    }

    it("should relay http requests") {
      val actor = system.actorOf(ApiClient.props(endpoint, "testApp"))
      val req = WrappedHttpRequest.getUrl("http://google.com")
      actor ! req
      probe.expectMsg(ApiRequest(FetchRequest(req, "testApp")))
      system.stop(actor)
    }

  }

}