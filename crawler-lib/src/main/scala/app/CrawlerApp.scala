package org.blikk.crawler.app

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import akka.stream.FlowMaterializer
import akka.util.Timeout
import com.blikk.serialization.HttpProtos
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * The `CrawlerApp` is responsible for connecting to the crawler platform endpoint.
  * Use the `start` method to start consuming data.
  */
class CrawlerApp(appId: String)
  (implicit val system: ActorSystem) extends ImplicitLogging {

  implicit val askTimeout = Timeout(5.seconds)

  // Queue name is based on the appId and routing key is the appID
  val queue = RabbitData.queueForApp(appId)
  val exchange = RabbitData.DataExchange
  val routingKey : String = appId

  def start() : StreamContext[CrawlItem] = {
    val channel = RabbitData.createChannel()
    // Create the flow
    val publisherActor = system.actorOf(RabbitPublisher.props(
      channel, queue, exchange, routingKey), s"rabbitMQPublisher-${appId}")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    val flow = Source(publisher).map { element =>
      SerializationUtils.fromProto(HttpProtos.CrawlItem.parseFrom(element))
    }
    // Return a context
    val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))
    StreamContext(appId, flow, publisherActor)(system, channel, materializer)
  }


}