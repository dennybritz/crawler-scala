package org.blikk.contactapp

import akka.io._
import spray.can._
import spray.http._
import akka.stream._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.blikk.crawler.processors._
import org.jsoup.Jsoup
import scala.util.{Success, Failure}
import scala.concurrent.duration._

object Main extends App {

  // Get the API endpoint from the configuration
  // It is automatically set by the syste,
  val config = ConfigFactory.load()
  val apiEndpoint = config.getString("blikk.app.rabbitMQ.uri")

  implicit val system = ActorSystem("contact-app")
  val jobManager = system.actorOf(JobManager.props(apiEndpoint), "jobManager")
  val api = system.actorOf(HttpApi.props(jobManager), "api")
  IO(Http) ! Http.Bind(api, "localhost", 9090)

  // jobManager ! StartJob("https://www.relateiq.com/", "", 2.minutes)

  system.awaitTermination()
  
}