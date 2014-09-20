package org.blikk.crawler

import akka.pattern.{ask, pipe}
import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

object ApiLayer {
  def props(crawlService: ActorRef) = Props(classOf[ApiLayer], crawlService)
}

class ApiLayer(crawlService: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  implicit val askTimeout = Timeout(5.seconds)
  val config = context.system.settings.config

  override def preStart(){
    log.info("started for crawlService {}", crawlService.path.toString)
  }

  def receive = {
    case ApiRequest(ConnectionInfoRequest) =>
      val connInfo = config.getString("blikk.rabbitMQ.uri")
      log.info("sending rabbitMQ information to {}: {}", sender, connInfo)
      sender ! ApiResponse(ConnectionInfo(connInfo))
    case ApiRequest(msg: FetchRequest) =>
      crawlService ! AddToFrontier(msg)
      sender ! ApiResponse.OK
    case ApiRequest(msg) =>
      log.warning("unhandled message type: {}", msg)
      sender ! ApiError("unhandled message type")
    case msg => 
      log.warning("message was not wrapped in an Api request, ignoring: {}", msg)
      sender ! ApiError("message was not wrapped in an Api request, ignoring")
  }


}