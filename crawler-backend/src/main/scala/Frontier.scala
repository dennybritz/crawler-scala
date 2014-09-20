package org.blikk.crawler

import akka.actor._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.stream.OverflowStrategy
import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MutableMap}
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
  extends Actor with ActorLogging with ImplicitFlowMaterializer {

  import Frontier._
  import context.dispatcher

  val rabbitChannel = rabbitConn.createChannel()
  val rabbitRoutingKey = "#"

  // Keep track of when we can politely send out the next request to each domain
  val nextRequestTime = MutableMap[String, Long]().withDefaultValue(System.currentTimeMillis)
  val defaultDelay = 250 // 250ms default delay for requests to the same domain

  override def preStart() {
    // Declare the necessary queues and exchanges
    log.info("""declaring RabbitMQ exchange "{}" and queues "{}", "{}" """, FrontierExchange.name, 
      FrontierQueue.name, FrontierScheduledQueue.name)
    rabbitChannel.exchangeDeclare(FrontierExchange.name, FrontierExchange.exchangeType, 
      FrontierExchange.durable)
    rabbitChannel.queueDeclare(FrontierQueue.name, FrontierQueue.durable, 
      FrontierQueue.exclusive, FrontierQueue.autoDelete, FrontierQueue.options)
    rabbitChannel.queueDeclare(FrontierScheduledQueue.name, FrontierScheduledQueue.durable, 
      FrontierScheduledQueue.exclusive, FrontierScheduledQueue.autoDelete, 
      FrontierScheduledQueue.options)

    val publisherActor = context.actorOf(
      RabbitPublisher.props(rabbitConn.createChannel(), 
      FrontierQueue, FrontierExchange, rabbitRoutingKey))
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    FlowFrom(publisher).map { element =>
      SerializationUtils.deserialize[FetchRequest](element)
    }.groupBy(_.req.host.trim).withSink(ForeachSink { case(key, domainFlow) =>
      // We rate-limit the new domain flow
      log.info("starting new subscription for {}", key)
      val tickSrc = FlowFrom(0 millis, defaultDelay.millis, () => "tick")
      val zip = Zip[String, FetchRequest]
      FlowGraph { implicit b =>
        tickSrc ~> zip.left
        domainFlow.buffer(5000, OverflowStrategy.backpressure) ~> zip.right
        zip.out ~> ForeachSink[(String,FetchRequest)] { case(_, item) =>
          routeFetchRequestGlobally(item)
        }
      }.run()
    }).run()
    // We need to wait a while before the rabbit consumer is done with binding
    // to the queue. This is ugly, is there a nicer way?
    Thread.sleep(1000)
  }

  /* Additional actor behavior */
  def receive = {
    case msg @ AddToFrontier(req, scheduledTime, ignoreDeduplication) =>
      val requestTime = scheduledTime
      log.debug("adding to frontier: {} (scheduled: {})", req.req.uri.toString, requestTime)
      addToFrontier(req, requestTime, ignoreDeduplication)
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

  def routeFetchRequestGlobally(fetchReq: FetchRequest) : Unit = {
    log.debug("routing {}", fetchReq)
    target ! ConsistentHashableEnvelope(fetchReq, fetchReq.req.host)
  }

}