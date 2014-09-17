package org.blikk.crawler.client

import akka.pattern.{ask, pipe}
import org.blikk.crawler._
import akka.actor._
import akka.util.Timeout
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.Await

object ApiClient {
  def props(apiEndpoint: String, appId: String) = {
    Props(classOf[ApiClient], apiEndpoint, appId)
  }
}

class ApiClient(apiEndpoint: String, appId: String)
  extends Actor with ActorLogging {

  implicit val apiTimeout = Timeout(5.seconds)
  import context.dispatcher

  val apiActor = context.system.actorSelection(apiEndpoint)

  override def preStart() {
    log.info("Connecting to {}", apiEndpoint)
  }

  override def receive = {
    case ConnectionInfoRequest =>
      (apiActor ? ApiRequest(ConnectionInfoRequest)).mapTo[ApiResponse].map(_.payload) pipeTo sender
    case req: WrappedHttpRequest =>
      apiActor ? ApiRequest(FetchRequest(req, appId)) onComplete {
        case Success(ApiResponse.OK) => // Ok, nothing to do
        case Success(ApiError(reason)) => log.error("API error: {}", reason)
        case Failure(err) => log.error("API error: {}", err.toString)
        case other => log.error("Unhandled API response {}", other)
      }

  }

}