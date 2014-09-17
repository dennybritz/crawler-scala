package org.blikk.crawler.app

import akka.actor._
import akka.pattern.ask
import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.util.Timeout
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.apache.commons.lang3.SerializationUtils

class CrawlerApp(apiEndpoint: String, appId: String)
  (implicit system: ActorSystem)  {

  implicit val askTimeout = Timeout(5.seconds)

  // Queue name is based on the appId and routing key is the appID
  val queue = RabbitData.queueForApp(appId)
  val exchange = RabbitData.DataExchange
  val routingKey : String = appId

  val clientActor = system.actorOf(ApiClient.props(apiEndpoint, appId), "apiClient")

  // Initialize: Get the RabbitMQ connection information
  val infoF = (clientActor ? ConnectionInfoRequest).mapTo[ConnectionInfo]
  val connectionInfo = Await.result(infoF, askTimeout.duration)
  val rabbitCF = new ConnectionFactory()
  rabbitCF.setUri(connectionInfo.rabbitMQUri)

  def createContext[A]() : StreamContext[A] = {
    // Initialize a new RabbitMQ connection
    val connection = rabbitCF.newConnection()
    val channel = connection.createChannel()
    // Create the flow
    val publisherActor = system.actorOf(RabbitPublisher.props(
      channel, queue, exchange, routingKey), "rabbitMQPublisher")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    val flow = FlowFrom(publisher).map { element =>
      SerializationUtils.deserialize[A](element)
    }
    val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))
    StreamContext(flow, clientActor)(system, connection, materializer)
  }


}