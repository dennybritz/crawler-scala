package org.blikk.crawler.app

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

/** 
  * Communicates with the remote API endpoint to get connection information and send commands. 
  * The apiEndpoint URI should be of the form: `akka.tcp://<systemName>@<ip>:<port>/user/api`
  */
class ApiClient(apiEndpoint: String, appId: String) extends Actor with ActorLogging {

  implicit val apiTimeout = Timeout(5.seconds)
  import context.dispatcher

  // Connects to the crawler platform API actor using Akka remoting
  var apiActor : ActorSelection = null

  override def preStart() {
    log.info("connecting to {}...", apiEndpoint)
    apiActor = context.system.actorSelection(apiEndpoint)
  }

  override def receive = {
    case ConnectionInfoRequest =>
      (apiActor ? ApiRequest(ConnectionInfoRequest)).mapTo[ApiResponse]
        .map(_.payload) pipeTo sender
    case req: WrappedHttpRequest =>
      log.debug("Adding to frontier: {}", req.uri.toString)
      apiActor ? ApiRequest(FetchRequest(req, appId)) onComplete {
        case Success(ApiResponse.OK) => // Ok, nothing to do
        case Success(ApiError(reason)) => log.error("API error: {}", reason)
        case Failure(err) => log.error("API error: {}", err.toString)
        case other => log.error("Unhandled API response {}", other)
      }

  }

}