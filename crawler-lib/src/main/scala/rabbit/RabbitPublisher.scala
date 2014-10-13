package org.blikk.crawler

import akka.actor._
import akka.stream.actor._
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.collection.JavaConversions._
import scala.annotation.tailrec

object RabbitPublisher {
  case object CompleteStream
  def props(channel: Channel, queue: RabbitQueueDefinition, 
    exchange: RabbitExchangeDefinition, routingKey: String) = 
    Props(classOf[RabbitPublisher], channel, queue, exchange, routingKey)
}

/** 
  * Binds a queue RabbitMQ exchange and publishes all received data. 
  * This actor can be transformed into a reactive flow.
  */ 
class RabbitPublisher(channel: Channel, queue: RabbitQueueDefinition, 
    exchange: RabbitExchangeDefinition, routingKey: String) extends Actor with ActorLogging 
  with ActorPublisher[Array[Byte]] {

  // Keeps track of the assigned queue and consumer tag
  var assignedQueue : String = ""
  var consumerTag : String = ""

  // The actual consumer object using RabbitMQ libraries
  val consumer =  new RabbitConsumer(channel, self)(context.system)

  var buf = Vector.empty[RabbitMessage]

  override def preStart(){
    log.info("susbcribing consumer to RabbitMQ queue=\"{}\"...", queue.name)
    assignedQueue = channel.queueDeclare(queue.name, queue.durable, 
      queue.exclusive, queue.autoDelete, queue.options).getQueue
    if (exchange != RabbitData.DefaultExchange)
      channel.queueBind(assignedQueue, exchange.name, routingKey)
    log.info("bound queue {} to exchange {}", assignedQueue, exchange.name)
    // No autoack
    consumerTag = channel.basicConsume(assignedQueue, false, consumer)
    log.info("susbcribed to queue {}", assignedQueue)
  }

  override def postStop(){
    // We unbding the queue and close the the channel if it's still open
    if (channel.isOpen){
      log.info("cancelling rabbitMQ consumption for {}", consumerTag)
      channel.basicCancel(consumerTag)
      log.info("unbinding rabbitMQ queue {}", assignedQueue)
      if (exchange != RabbitData.DefaultExchange) 
        channel.queueUnbind(queue.name, exchange.name, routingKey)
    }
  }

  def receive = {
    case x : RabbitMessage => processItem(x)
    case RabbitPublisher.CompleteStream => onComplete()
    case ActorPublisherMessage.Request(_) => deliverBuffer()
    case ActorPublisherMessage.Cancel => deliverBuffer(); onComplete()
    case msg : ActorPublisherMessage => // Nothing to do
    case msg => log.warning("unhandled message: {}", msg) 
  }

  def processItem(x: RabbitMessage) {
    buf :+= x
    deliverBuffer()
    channel.basicAck(x.deliveryTag, false)
  }

  def deliverBuffer() {
    log.debug("Delivering bufferSize={} with demand={}", buf.size, totalDemand.toInt)
    if (totalDemand > 0) {
      val (use, keep) = buf.splitAt(totalDemand.toInt)
      buf = keep
      use.map(_.payload).foreach(onNext)
    }
  }
}
