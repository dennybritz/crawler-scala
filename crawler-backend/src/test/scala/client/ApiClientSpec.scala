package org.blikk.test

import org.blikk.crawler.client._
import org.blikk.crawler._
import akka.actor._
import akka.testkit._

class ApiClientSpec extends AkkaSingleNodeSpec("ApiClientSpec") {

  val probe = TestProbe()
  val endpoint = probe.ref.path.toString

  describe("API Client") {
    it("should be able to get connection information") {
      val actor = system.actorOf(ApiClient.props(endpoint, "testApp"), "apiClient")
      actor ! ConnectionInfoRequest
      probe.expectMsg(ConnectionInfoRequest)
      probe.reply(ConnectionInfo("someUri"))
      expectMsg(ConnectionInfo("someUri"))
    }
  }

}