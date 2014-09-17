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
}

class Frontier(rabbitConn: RabbitConnection, target: ActorRef) 
  extends Actor with ActorLogging {

  import context.dispatcher

  val rabbitExchangeName = "blikk-frontier-exchange"
  val rabbitSchedulerQueue = "blikk-frontier-queue-scheduled"
  val rabbitQueueName = "blikk-frontier-queue"
  val rabbitChannel = rabbitConn.createChannel()
  val rabbitRoutingKey = "*"
  val urlCacheKey = s"blikk:frontier:urlCache"


  override def preStart() {
    // Declare the necessary queues and exchanges
    log.info("""declaring RabbitMQ exchange "{}"" and queues "{}", "{}" """, rabbitExchangeName, 
      rabbitQueueName, rabbitSchedulerQueue)
    rabbitChannel.exchangeDeclare(rabbitExchangeName, "topic", false)
    rabbitChannel.queueDeclare(rabbitQueueName, false, false, false, null)
    rabbitChannel.queueDeclare(rabbitSchedulerQueue, false, false, false, 
      Map("x-dead-letter-exchange" -> rabbitExchangeName))

    implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(context.system))
    val publisherActor = context.actorOf(
      RabbitPublisher.props(rabbitConn.createChannel(), rabbitQueueName, 
      rabbitExchangeName, "*"))
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
      rabbitChannel.queuePurge(rabbitQueueName)
    }
  }

  /* Add a new request to the frontier */
  def addToFrontier(fetchReq: FetchRequest, scheduledTime: Option[Long],
    ignoreDeduplication: Boolean = false) : Unit = {
    val serializedMsg = SerializationUtils.serialize(fetchReq)
    scheduledTime.map(_ - System.currentTimeMillis) match {
      case Some(delay) if delay > 0 =>
        val properties = new AMQP.BasicProperties.Builder().expiration(delay.toString).build()
        rabbitChannel.basicPublish("", rabbitSchedulerQueue, properties, serializedMsg)
      case _ =>
        rabbitChannel.basicPublish(rabbitExchangeName, fetchReq.req.host, null, serializedMsg)
    }
  }

}