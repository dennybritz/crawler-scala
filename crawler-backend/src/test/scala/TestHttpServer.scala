package org.blikk.test

import org.blikk.crawler.Logging
import akka.pattern.ask
import akka.io.IO
import akka.actor._
import spray.can._
import spray.http._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import akka.util.Timeout

class HttpServerListener extends Actor with ActorLogging {

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/links/1"), _, _, _) =>
      log.debug("HTTP Request: {}", req.toString)
      sender ! HttpResponse(entity = """
      <html><body>
      <a href="/links/2">This is a link</a>
      </body></html>""")
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/links/2"), _, _, _) =>
      log.debug("HTTP Request: {}", req.toString)
      sender ! HttpResponse(entity = """
      <html><body>
      <a href="/links/3">This is a link</a>
      </body></html>""")
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/links/3"), _, _, _) =>
      log.debug("HTTP Request: {}", req.toString)
      sender ! HttpResponse(entity = """
      <html><body>
      </body></html>""")
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