package org.blikk.crawler

import akka.actor._
import akka.pattern.{ask, pipe}
import spray.http._
import spray.can.Http
import akka.io._
import akka.util.{Timeout}
import akka.contrib.throttle._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success}

trait HttpFetcher { this: Actor with ActorLogging =>
  
  implicit val askTimeout : Timeout

  implicit val _system = context.system
  implicit val _context = context.dispatcher

  def throttleRate : Throttler.Rate

  lazy val httpThrottler = {
    val result = context.actorOf(Props(classOf[TimerBasedThrottler], throttleRate))
    result ! Throttler.SetTarget(Some(IO(Http)))
    result
  }
  
  def dispatchHttpRequest(req: WrappedHttpRequest, jobId: String, sender: ActorRef) : Unit = {
    val futureResponse = httpThrottler ? req.req
    (futureResponse.mapTo[HttpResponse]) map { res => 
      FetchResponse(new WrappedHttpResponse(res), req, jobId) 
    } pipeTo sender
  }

}