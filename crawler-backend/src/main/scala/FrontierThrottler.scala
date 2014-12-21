package org.blikk.crawler

import akka.actor._
import scala.concurrent.duration._
import akka.stream.scaladsl._
import akka.stream.actor._
import com.blikk.serialization.HttpProtos
import org.blikk.crawler.processors._

object FrontierThrottler {
  def props(timeout: Int = 100) = Props(classOf[FrontierThrottler], timeout)
}

class FrontierThrottler(scheduleTimeout: Int) extends RabbitThrottler 
  with ActorPublisher[FetchRequest] with ImplicitFlowMaterializer {

  import context.system

  var scheduleCounter = Map[String, Int]().withDefault(_ => 0)
  def rabbitExchange = RabbitData.FrontierExchangeThrottled

  override def preStart() = {
  
    // Publishes all request coming into the frontier. These requests are not throttled yet. 
    val requestPublisherActor = context.actorOf(
      RabbitPublisher.props(
        RabbitData.createChannel(), RabbitData.FrontierQueue, RabbitData.FrontierExchange, "#"), 
      "requestPublisherActor")
    val requestSource = Source(ActorPublisher[Array[Byte]](requestPublisherActor))

    // A sink that routes request to the appropriate queues
    val routeSink = RabbitMQSink.build[FetchRequest](
      RabbitData.createChannel(), rabbitExchange) { fetchReq =>
      val serializedItem = SerializationUtils.toProto(fetchReq).toByteArray
      (serializedItem, fetchReq.req.topPrivateDomain)
    }

    // We route each request to the appropriate queue, creating the queue if it doesn't exist yet.
    requestSource via Flow[Array[Byte]].map { element =>
      val fetchReq = SerializationUtils.fromProto(HttpProtos.FetchRequest.parseFrom(element))
      val tpd = fetchReq.req.topPrivateDomain
      val interval = Config.customDomainDelays.get(tpd).getOrElse(Config.perDomainDelay)
      addQueue(tpd, RabbitQueueDefinition(s"com.blikk.crawler.requests.${tpd}", false, true))
      self ! RabbitThrottler.AddSchedule(tpd, interval, interval)
      fetchReq
    } runWith(routeSink)

  }

  def frontierThrottlerBehavior : Receive  = {
    case RabbitPublisher.CompleteStream => 
      onComplete()
      log.info("stream completed")
    case ActorPublisherMessage.Request(_) =>
      // We determine when to send messages through scheduling, nothing to do here.
    case ActorPublisherMessage.Cancel => 
      onComplete()
      log.info("stream cancelled")
    case msg : ActorPublisherMessage => // Nothing to do
  }

  def receive = frontierThrottlerBehavior orElse rabbitThrottlerBehavior

  def handleItem(routingKey: String, item: RabbitMessage) : Unit = {
    log.debug("{},{},{}", routingKey, isActive, totalDemand)
    if (isActive && totalDemand > 0) {
      val deserializedMessage = SerializationUtils.fromProto(HttpProtos.FetchRequest.parseFrom(item.payload))
      rabbitChannel.basicAck(item.deliveryTag, false)
      onNext(deserializedMessage)
    } else {
      // Requeue the message
      rabbitChannel.basicNack(item.deliveryTag, false, true)
    }
  }

  def handleNoItem(routingKey: String) : Unit = {
    scheduleCounter += Tuple2(routingKey, scheduleCounter(routingKey) + 1)
    if (scheduleCounter(routingKey) >= scheduleTimeout){
      log.info("Timeout={} for tpd='{}' reached, cancelling schedule.", scheduleTimeout, routingKey)
      self ! RabbitThrottler.RemoveSchedule(routingKey)
    }
  }

}
