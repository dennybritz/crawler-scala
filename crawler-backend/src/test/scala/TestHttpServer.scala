package org.blikk.test

import org.blikk.crawler.Logging
import akka.pattern.ask
import akka.io.IO
import akka.actor._
import spray.can._
import spray.http._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout

class HttpServerListener extends Actor with ActorLogging {

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)
    case req: HttpRequest =>
      log.debug("HTTP Request: {}", req.toString)
      sender() ! HttpResponse(StatusCodes.OK)
  }

}

object TestHttpServer extends Logging {

  implicit val askTimeout = Timeout(1 seconds)

  def start(interface: String, port: Int)(implicit system: ActorSystem) : Unit = {
    // TODO: Get and save actor to shut down server later
    val listener = system.actorOf(Props[HttpServerListener])
    val boundFuture = (IO(Http) ? Http.Bind(listener, interface, port)).mapTo[Http.Bound]
    val boundResult = Await.result(boundFuture, 1.seconds)
    log.info(s"HTTP server started: ${boundResult.toString}")
  }

}