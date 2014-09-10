package org.blikk.test

import akka.actor.{Actor, ActorSystem, ActorLogging, Props, ActorRef}
import akka.contrib.throttle.Throttler
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestActorRef }
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll}
import org.blikk.crawler._
import spray.http.{HttpRequest => SprayHttpRequest, Uri}
import spray.http.HttpMethods._
import scala.concurrent.duration._
import akka.util.Timeout

class TestHttpFetcher extends Actor with ActorLogging with HttpFetcher {
  implicit val askTimeout = Timeout(5 seconds)
  val throttleRate = Throttler.Rate(5, 1.seconds)
  def receive = {
    case _ => // Ignore
  } 
}

class HttpFetcherSpec extends AkkaSingleNodeSpec("HttpFetcherSpec") {

  describe("HttpFetcher") {

    it("should execute HttpFetchRequests and receive a FetchResponse for HTTP 200") {
      val fetcher = TestActorRef[TestHttpFetcher]
      fetcher.underlyingActor.dispatchHttpRequest(
        WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob", self)
      expectMsgType[FetchResponse]
    }

  }

}