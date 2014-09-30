package org.blikk.contactapp

import akka.actor._
import akka.io._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import spray.can._
import spray.http._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.routing._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import DefaultJsonProtocol._

object HttpApi {
  case class Job(url: String, callbackUrl: String)
  implicit val phoneWrites = Json.format[Job]

  def props(jobManager: ActorRef) = Props(classOf[HttpApi], jobManager)
}

class HttpApi(jobManager: ActorRef) extends HttpServiceActor with ActorLogging {

  import HttpApi._
  import context.dispatcher

  implicit val askTimeout = Timeout(5.seconds) 

  def receive = runRoute {
    post {
      path("jobs") {
        entity(as[String]) { j =>
          val job = Json.parse(j).validate[Job].asOpt.get
          val appIdF = (jobManager ? StartJob(job.url, job.callbackUrl, 30.seconds)).mapTo[String]
          complete(appIdF)
        }
      }
    } 
  }

}
