package org.blikk.crawler

import akka.actor._
import akka.stream.actor._
import com.rabbitmq.client._
import org.blikk.crawler._
import scala.collection.JavaConversions._

object RabbitPublisher {
  def props(channel: Channel, queue: RabbitQueueDefinition, 
    exchange: RabbitExchangeDefinition, routingKey: String) = 
    Props(classOf[RabbitPublisher], channel, queue, exchange, routingKey)
}

class RabbitPublisher(channel: Channel, queue: RabbitQueueDefinition, 
    exchange: RabbitExchangeDefinition, routingKey: String) extends Actor with ActorLogging 
  with ActorPublisher[Array[Byte]] {

  var assignedQueue : String = ""
  var consumerTag : String = ""
  val consumer =  new RabbitConsumer(channel, self)(context.system)

  override def preStart(){
    log.info("susbcribing consumer to RabbitMQ queue...")
    // non-durable, non-exclusive, non-autodelete queue
    assignedQueue = channel.queueDeclare(queue.name, queue.durable, 
      queue.exclusive, queue.autoDelete, queue.options).getQueue
    channel.queueBind(assignedQueue, exchange.name, routingKey)
    log.info("bound queue {} to exchange {}", assignedQueue, exchange.name)
    // No autoack
    consumerTag = channel.basicConsume(assignedQueue, false, consumer)
    log.info("susbcribed to queue {}", assignedQueue)
  }

  override def postStop(){
    // We unbding and delete the queue
    if (channel.isOpen){
      log.info("cancelling rabbitMQ consumption for {}", consumerTag)
      channel.basicCancel(consumerTag)
      log.info("unbinding rabbitMQ queue {}", assignedQueue)
      channel.queueUnbind(queue.name, exchange.name, routingKey)
      channel.close()
    }
  }

  def receive = {
    case x : RabbitMessage =>
      processItem(x)
  }

  def processItem(x: RabbitMessage) {
    log.debug("processing tag=\"{}\"", x.deliveryTag)
    if (isActive && totalDemand > 0) {
      onNext(x.payload)
      channel.basicAck(x.deliveryTag, false)
    } else {
      channel.basicNack(x.deliveryTag, false, true)
    }
  }

}