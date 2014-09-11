package org.blikk.crawler

import akka.pattern.{ask, pipe}
import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._

object ApiLayer {
  def props(crawlService: ActorRef) = Props(classOf[ApiLayer], crawlService)
}

class ApiLayer(crawlService: ActorRef) extends Actor with ActorLogging {

  implicit val askTimeout = Timeout(5.seconds)
  import context.dispatcher

  def receive = {
    case ApiRequest(msg: RunJob) =>
      crawlService ! msg
      sender ! ApiResponse("ok")
    case ApiRequest(msg: StopJob) =>
      crawlService ! msg
      sender ! ApiResponse("ok")
    case ApiRequest(msg: DestroyJob) =>
      crawlService ! msg
      sender ! ApiResponse("ok")
    case ApiRequest(msg: GetGlobalJobStats) =>
      (crawlService ? msg).map( result => ApiResponse(result)) pipeTo sender
    case ApiRequest(msg) =>
      log.warning("unhandled message type: {}", msg)
      sender ! ApiError("unhandled message type")
    case msg => 
      log.warning("message was not wrapped in an Api request, ignoring: {}", msg)
      sender ! ApiError("message was not wrapped in an Api request, ignoring")
  }


}