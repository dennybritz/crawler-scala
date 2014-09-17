package org.blikk.crawler.client

import akka.actor._
import akka.pattern.ask
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.util.Timeout
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.apache.commons.lang3.SerializationUtils

class CrawlerClient(apiEndpoint: String, appId: String, 
  val exchangeName: String = "blikk-data")
  (implicit system: ActorSystem)  {

  implicit val askTimeout = Timeout(5.seconds)

  // Empty queue name: Let the server create a temporary queue
  def queueName : String = ""
  def routingKey : String = appId

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
      channel, queueName, exchangeName, routingKey), "rabbitMQPublisher")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    val flow = Flow(publisher).map { element =>
      SerializationUtils.deserialize[A](element)
    }
    val materializer = FlowMaterializer(MaterializerSettings(system))
    StreamContext(flow, clientActor)(system, connection, materializer)
  }


}