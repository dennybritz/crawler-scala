package org.blikk.crawler

import akka.actor._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.stream.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import com.blikk.serialization.HttpProtos
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import org.blikk.crawler.processors.ThrottleTransformer

object Frontier {
  def props(target: ActorRef) = {
    Props(classOf[Frontier], target)
  }
}

class Frontier(target: ActorRef)
  extends Actor with ActorLogging with ImplicitFlowMaterializer {

  import RabbitData._
  import context.dispatcher

  val rabbitChannel = RabbitData.createChannel()
  val rabbitRoutingKey = "#"

  override def preStart() {
    val publisherActor = context.actorOf(
      RabbitPublisher.props(RabbitData.createChannel(),
      FrontierQueue, FrontierExchange, rabbitRoutingKey), s"frontierRabbit")
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    val throttler = new GroupThrottler[FetchRequest](Config.perDomainDelay, 
      Config.customDomainDelays)(_.req.topPrivateDomain.getOrElse(""))

    Source(publisher).map { element =>
      SerializationUtils.fromProto(HttpProtos.FetchRequest.parseFrom(element))
    }.timerTransform("throttle", () => throttler)
    .to(Sink.foreach[FetchRequest](routeFetchRequestGlobally))
    .run()

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
    Resource.using(RabbitData.createChannel()) { channel =>
      log.info("clearing frontier")
      rabbitChannel.queuePurge(FrontierQueue.name)
      rabbitChannel.queuePurge(FrontierScheduledQueue.name)
    }
  }

  /* Add a new request to the frontier */
  def addToFrontier(fetchReq: FetchRequest, scheduledTime: Option[Long],
    ignoreDeduplication: Boolean = false) : Unit = {
    val serializedMsg = SerializationUtils.toProto(fetchReq).toByteArray
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
    /*
     * We used to route the fetch request to nodes based on consistent hashing on the hostname.
     * This resulted in the same domains being handled by the same nodes in the cluster, which
     * is good for DNS caching but results in long delays since we will always use the
     * same IP address to make requestes to that host.
     *
     * We now simply execute the request locally. Thus, every node in the cluster
     * can make requestes to every domain. Because of the way RabbitMQ works requests are
     * taken from the queue in a round-robin fashion, which also acts a simple form of load balancing.
     * This approach is likely to work fine with as long as we stick with focused crawls.
     * If we get into broad crawling we likely need to start worrying about DNS partitioning again.
     */
    // target ! ConsistentHashableEnvelope(fetchReq, fetchReq.req.host)
    target ! fetchReq
  }

}
