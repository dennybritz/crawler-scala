package org.blikk.crawler.processors

import akka.actor._
import akka.stream.actor._
import akka.stream.scaladsl._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import org.blikk.crawler.{RabbitData, RabbitExchangeDefinition}
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions._

object RabbitMQSink {

  type PublishItem = Tuple2[Array[Byte], String]

  def props[A](channel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition) 
    (ser: A => PublishItem) = Props(classOf[RabbitMQSink[A]], channel, rabbitExchange, ser)

  def build[A](channel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition) 
    (ser: A => PublishItem)(implicit system: ActorSystem) : Sink[A] = {
      val rabbitSinkActor = system.actorOf(RabbitMQSink.props(channel, rabbitExchange)(ser))
      Sink(ActorSubscriber[A](rabbitSinkActor))
  }
}

/**
  * Subscribes to the stream of items and publishes them into RabbitMQ
  */
class RabbitMQSink[A](rabbitMQChannel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition)
  (ser: A => Tuple2[Array[Byte], String]) extends Actor with ActorLogging with ActorSubscriber {

  import ActorSubscriberMessage._
  import RabbitMQSink._

  // TODO: What is a good value?
  def requestStrategy = WatermarkRequestStrategy(100)

  override def preStart(){
    log.info("starting")
    log.info("initializing RabbitMQ exchange {}", rabbitExchange.name)
    if (rabbitExchange != RabbitData.DefaultExchange)
      rabbitMQChannel.exchangeDeclare(rabbitExchange.name, rabbitExchange.exchangeType, 
        rabbitExchange.durable, rabbitExchange.autoDelete,
        rabbitExchange.internal, rabbitExchange.arguments)
    log.info("started")
  }

  def receive = {
    case next @ OnNext(item) => 
      Try(item.asInstanceOf[A]) match {
        case Success(item) => writeData(item)
        case Failure(item) => log.error("Received unexpected type: {}", item)
      }
    case complete @ OnComplete =>
      log.info("stream is finished. shutting down.")
      context.stop(self)
    case err @ OnError(cause) =>
      log.error("stream error, shutting down: {}", cause.toString)
      context.stop(self)
  }

  /* Writes the item to RabbitMQ */
  def writeData(item: A) : Unit = {
    val record = ser(item)
    val Tuple2(serializedItem, routingKey) = record
    log.info("writing numBytes={} to RabbitMQ exchange=\"{}\" routingKey=\"{}\"", 
    serializedItem.size, rabbitExchange.name, routingKey)
    rabbitMQChannel.basicPublish(rabbitExchange.name, routingKey, null, serializedItem)
  }


}