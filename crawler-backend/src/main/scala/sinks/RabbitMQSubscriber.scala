package org.blikk.crawler

import akka.actor._
import akka.stream.actor._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import org.apache.commons.lang3.SerializationUtils

object RabbitMQSubscriber {
  def props(conn: RabbitConnection) = Props(classOf[RabbitMQSubscriber], conn)
}

/**
  * Subscribes to the stream of request-response objects and publishes them into RabbitMQ
  */
class RabbitMQSubscriber(conn: RabbitConnection) extends Actor with ActorLogging 
  with ActorSubscriber {

  import ActorSubscriberMessage._

  // Is 100 a good value? I have no idea.
  def requestStrategy = WatermarkRequestStrategy(100)

  // Use a different channel on each thread
  lazy val rabbitMQChannel = new ThreadLocal[RabbitChannel] {
    override def initialValue = conn.createChannel()
  }

  override def preStart(){
    log.info("starting")
    log.info("initializing RabbitMQ exchange {}", RabbitData.DataExchange.name)
    rabbitMQChannel.get().exchangeDeclare(RabbitData.DataExchange.name, 
      RabbitData.DataExchange.exchangeType, RabbitData.DataExchange.durable) 
  }

  def receive = {
    case next @ OnNext(item: FetchResponse) => writeFetchResponse(item)
    case complete @ OnComplete =>
      log.info("stream is finished. shutting down.")
      context.stop(self)
    case err @ OnError(cause) =>
      log.error("stream error, shutting down: {}", cause.toString)
      context.stop(self)
  }

  /* Writes a CrawlItem to RabbitMQ */
  def writeFetchResponse(fetchRes: FetchResponse) : Unit = {
    val item = CrawlItem(fetchRes.fetchReq.req, fetchRes.res)
    val serializedItem = SerializationUtils.serialize(item)
    val channel = rabbitMQChannel.get()
    log.debug("writing numBytes={} to RabbitMQ", serializedItem.size)
    channel.basicPublish(RabbitData.DataExchange.name, fetchRes.fetchReq.appId, null, serializedItem)
  }


}