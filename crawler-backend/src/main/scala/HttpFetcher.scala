package org.blikk.crawler

import akka.actor._
import akka.pattern.{ask, pipe}
import spray.http._
import spray.can.Http
import akka.io._
import akka.util.{Timeout}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success}

trait HttpFetcher extends ActorLogging { this: Actor =>
  
  implicit val askTimeout : Timeout

  implicit val _system = context.system
  implicit val _context = context.dispatcher

  def dispatchHttpRequest(req: WrappedHttpRequest, jobId: String, sender: ActorRef) : Unit = {
    val futureResponse = IO(Http) ? req.req
    (futureResponse.mapTo[HttpResponse]) map { res => 
      FetchResponse(new WrappedHttpResponse(res), req, jobId) 
    } pipeTo sender
  }

}