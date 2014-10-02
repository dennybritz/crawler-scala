package org.blikk.crawler.app

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl2._
import akka.util.Timeout
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * The `CrawlerApp` is responsible for connecting to the crawler platform endpoint.
  * Use the `start` method to start consuming data.
  */
class CrawlerApp(rabbitMQUri: String, appId: String)
  (implicit val system: ActorSystem) extends ImplicitLogging {

  implicit val askTimeout = Timeout(5.seconds)

  // Queue name is based on the appId and routing key is the appID
  val queue = RabbitData.queueForApp(appId)
  val exchange = RabbitData.DataExchange
  val routingKey : String = appId

  // Initialize: Get the RabbitMQ connection information
  val rabbitCF = new ConnectionFactory()
  rabbitCF.setUri(rabbitMQUri)

  def start[A]() : StreamContext[A] = {
    // Initialize a new RabbitMQ connection
    val connection = rabbitCF.newConnection()
    val channel = connection.createChannel()
    // Create the flow
    val publisherActor = system.actorOf(RabbitPublisher.props(
      channel, queue, exchange, routingKey), s"rabbitMQPublisher-${appId}")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    val flow = FlowFrom(publisher).map { element =>
      SerializationUtils.deserialize[A](element)
    }
    // Return a context
    val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))
    StreamContext(appId, flow, publisherActor)(system, connection, materializer)
  }


}