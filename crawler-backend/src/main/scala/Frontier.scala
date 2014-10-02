package org.blikk.crawler

import akka.actor._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.stream.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.immutable.Seq
import scala.concurrent.duration._

object Frontier {
  def props(rabbitConn: RabbitConnection, target: ActorRef) = {
    Props(classOf[Frontier], rabbitConn, target)
  }
}

class Frontier(rabbitConn: RabbitConnection, target: ActorRef) 
  extends Actor with ActorLogging with ImplicitFlowMaterializer {

  import RabbitData._
  import context.dispatcher

  val rabbitChannel = rabbitConn.createChannel()
  val rabbitRoutingKey = "#"

  // Delay for requests to the same domain
  val defaultDelay = 750 
  // We can keep this many messages per domain in a buffer 
  val perDomainBuffer = 5000

  override def preStart() {
    val publisherActor = context.actorOf(
      RabbitPublisher.props(rabbitConn.createChannel(), 
      FrontierQueue, FrontierExchange, rabbitRoutingKey), s"frontierRabbit")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)

    FlowFrom(publisher).map { element =>
      SerializationUtils.deserialize[FetchRequest](element)
    }.groupBy(_.req.host.trim).withSink(ForeachSink { case(key, domainFlow) =>
      log.info("starting new request stream for {}", key)
      val tickSrc = FlowFrom(0 millis, defaultDelay.millis, () => "tick")
      val zip = Zip[String, FetchRequest]
      FlowGraph { implicit b =>
        tickSrc.buffer(1, OverflowStrategy.dropTail) ~> zip.left
        domainFlow.buffer(perDomainBuffer, OverflowStrategy.backpressure) ~> zip.right
        zip.out ~> FlowFrom[(String, FetchRequest)].map(_._2)
          .withSink(ForeachSink[FetchRequest](routeFetchRequestGlobally))
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
    log.debug("routing {}", fetchReq.req.uri.toString)
    target ! ConsistentHashableEnvelope(fetchReq, fetchReq.req.host)
  }

}