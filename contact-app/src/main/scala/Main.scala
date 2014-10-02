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

  // Get the API endpoint from the configuration
  // It is automatically set by the syste,
  val config = ConfigFactory.load()
  val rabbitMQUri = config.getString("blikk.app.rabbitMQ.uri")

  implicit val system = ActorSystem("contact-app")
  val jobManager = system.actorOf(JobManager.props(rabbitMQUri), "jobManager")

  // Create a Flow for reading jobs from RabbitMQ
  val rabbitFactory = new RabbitMQCF()
  rabbitFactory.setUri(rabbitMQUri)
  val rabbitConn = rabbitFactory.newConnection()
  val requestQueue = RabbitQueueDefinition("com.blikk.contactapp.requests", true, false, false)
  val requestActor = system.actorOf(RabbitPublisher.props(rabbitConn.createChannel(), requestQueue, 
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