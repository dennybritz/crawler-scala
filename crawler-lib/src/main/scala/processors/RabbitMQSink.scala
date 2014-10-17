package org.blikk.crawler.processors

import akka.actor._
import akka.stream.actor._
import akka.stream.scaladsl2._
import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}
import org.blikk.crawler.{RabbitData, RabbitExchangeDefinition}
import scala.util.{Try, Success, Failure}

object RabbitMQSink {
  def props[A](channel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition) 
    (ser: A => (Array[Byte], String)) = Props(classOf[RabbitMQSink[A]], channel, rabbitExchange, ser)

  def build[A](channel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition) 
    (ser: A => (Array[Byte], String))(implicit system: ActorSystem) : SubscriberDrain[A] = {
      val rabbitSinkActor = system.actorOf(RabbitMQSink.props(channel, rabbitExchange)(ser))
      SubscriberDrain(ActorSubscriber[A](rabbitSinkActor))
  }
}

/**
  * Subscribes to the stream of items and publishes them into RabbitMQ
  */
class RabbitMQSink[A](rabbitMQChannel: RabbitChannel, rabbitExchange: RabbitExchangeDefinition)
  (ser: A => (Array[Byte], String)) extends Actor with ActorLogging with ActorSubscriber {

  import ActorSubscriberMessage._

  // TODO: What is a good value?
  def requestStrategy = WatermarkRequestStrategy(100)

  override def preStart(){
    log.info("starting")
    log.info("initializing RabbitMQ exchange {}", rabbitExchange.name)
    if (rabbitExchange != RabbitData.DefaultExchange)
      rabbitMQChannel.exchangeDeclare(rabbitExchange.name, 
        rabbitExchange.exchangeType, rabbitExchange.durable) 
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
    val Tuple2(serializedItem, routingKey) = ser(item)
    log.info("writing numBytes={} to RabbitMQ exchange=\"{}\" routingKey=\"{}\"", 
      serializedItem.size, rabbitExchange.name, routingKey)
    rabbitMQChannel.basicPublish(rabbitExchange.name, routingKey, null, serializedItem)
  }


}