package org.blikk.contactapp

import akka.actor.ActorSystem
import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import com.rabbitmq.client.{Connection => RabbitMQConnection, ConnectionFactory => RabbitMQCF}
import com.typesafe.config.ConfigFactory
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.blikk.crawler.processors._
import scala.concurrent.duration._
import spray.can._
import spray.http._
import spray.json._
import JsonProtocols._


object Main extends App with ImplicitLogging {

  implicit val system = ActorSystem("contact-app")
  val jobManager = system.actorOf(JobManager.props, "jobManager")

  // Create a Flow for reading jobs from RabbitMQ
  val requestQueue = RabbitQueueDefinition("com.blikk.contactapp.requests", true, false, false)
  val requestActor = system.actorOf(RabbitPublisher.props(RabbitData.createChannel(), requestQueue, 
    RabbitData.DefaultExchange, "com.blikk.contactapp.requests"), "requestPublisher")
  val requestFlow = FlowFrom(ActorPublisher[Array[Byte]](requestActor))
  
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))
  requestFlow.withSink(ForeachSink[Array[Byte]] { item =>
    val requestObj = new String(item).parseJson.convertTo[Request]
    log.info("New request url=\"{}\" appId=\"{}\"", requestObj.url, requestObj.appId)
    jobManager ! StartJob(requestObj.url, requestObj.appId, 30.seconds)
  }).run()

  system.awaitTermination()
  
}