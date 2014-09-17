package org.blikk.crawler

import akka.actor._
import akka.stream.actor._
import akka.stream.scaladsl2._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import org.apache.commons.lang3.SerializationUtils
import scala.collection.JavaConversions._
import scala.concurrent.duration._

object Frontier {
  def props(rabbitConn: RabbitConnection, target: ActorRef) = {
    Props(classOf[Frontier], rabbitConn, target)
  }
  val FrontierExchange = RabbitExchangeDefinition("blikk-frontier-exchange", "topic", true)
  val FrontierQueue = RabbitQueueDefinition("blikk-frontier-queue", true, false, false, Map.empty)
  val FrontierScheduledQueue = RabbitQueueDefinition("blikk-frontier-queue-scheduled", true,
    false, false, Map("x-dead-letter-exchange" -> FrontierExchange.name))
}

class Frontier(rabbitConn: RabbitConnection, target: ActorRef) 
  extends Actor with ActorLogging {

  import Frontier._
  import context.dispatcher

  val rabbitChannel = rabbitConn.createChannel()
  val rabbitRoutingKey = "*"

  override def preStart() {
    // Declare the necessary queues and exchanges
    log.info("""declaring RabbitMQ exchange "{}"" and queues "{}", "{}" """, FrontierExchange, 
      FrontierQueue, FrontierScheduledQueue)
    rabbitChannel.exchangeDeclare(FrontierExchange.name, FrontierExchange.exchangeType, 
      FrontierExchange.durable)
    rabbitChannel.queueDeclare(FrontierQueue.name, FrontierQueue.durable, 
      FrontierQueue.exclusive, FrontierQueue.autoDelete, FrontierQueue.options)
    rabbitChannel.queueDeclare(FrontierScheduledQueue.name, FrontierScheduledQueue.durable, 
      FrontierScheduledQueue.exclusive, FrontierScheduledQueue.autoDelete, 
      FrontierScheduledQueue.options)

    implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(context.system))
    val publisherActor = context.actorOf(
      RabbitPublisher.props(rabbitConn.createChannel(), 
      FrontierQueue, FrontierExchange, "*"))
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    FlowFrom(publisher).map { element =>
      SerializationUtils.deserialize[FetchRequest](element)
    }.withSink(ForeachSink { item => 
      target ! RouteFetchRequest(item)
    }).run()
  }

  /* Additional actor behavior */
  def receive = {
    case AddToFrontier(req, scheduledTime, ignoreDeduplication) =>
      log.info("adding to frontier: {} (scheduled: {})", req.req.uri.toString, scheduledTime)
      addToFrontier(req, scheduledTime, ignoreDeduplication)
    case ClearFrontier =>
      log.info("clearing frontier")
      clearFrontier()
  }

  /* Removes all elements from the frontier. Use with care! */
  def clearFrontier() : Unit = {
    Resource.using(rabbitConn.createChannel()) { channel =>
      log.info("clearing frontier")
      rabbitChannel.queuePurge(FrontierQueue.name)
      rabbitChannel.queuePurge(FrontierScheduledQueue.name)
    }
  }

  /* Add a new request to the frontier */
  def addToFrontier(fetchReq: FetchRequest, scheduledTime: Option[Long],
    ignoreDeduplication: Boolean = false) : Unit = {
    val serializedMsg = SerializationUtils.serialize(fetchReq)
    scheduledTime.map(_ - System.currentTimeMillis) match {
      case Some(delay) if delay > 0 =>
        val properties = new AMQP.BasicProperties.Builder().expiration(delay.toString).build()
        rabbitChannel.basicPublish("", FrontierScheduledQueue.name, properties, serializedMsg)
      case _ =>
        rabbitChannel.basicPublish(FrontierExchange.name, fetchReq.req.host, null, serializedMsg)
    }
  }

}