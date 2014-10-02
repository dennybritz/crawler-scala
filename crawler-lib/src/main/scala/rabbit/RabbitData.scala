package org.blikk.crawler

import com.rabbitmq.client.{Channel => RabbitChannel}
import scala.collection.JavaConversions._

/* RabbotMQ queue properties */
case class RabbitQueueDefinition(
  name: String, 
  durable: Boolean, 
  exclusive: Boolean = false, 
  autoDelete: Boolean = false, 
  options: Map[String, Object] = Map.empty)

/* RabbitMQ exchange properties */
case class RabbitExchangeDefinition(
  name: String, 
  exchangeType: String, 
  durable: Boolean)

/** 
  * RabbitMQ data definitions shared by several classes 
  * Note: server-side definition are not included
  */
object RabbitData extends Logging {

  // The RabbitMQ default exchange
  val DefaultExchange = RabbitExchangeDefinition("", "direct", true)

  // Used to exchange messages between crawl platform and crawl apps
  val DataExchange = RabbitExchangeDefinition("com.blikk.crawler.data-x", "direct", true)

  def queueForApp(appId: String) = 
    RabbitQueueDefinition(s"${appId}", true, false, false, Map.empty)

  val FrontierExchange = RabbitExchangeDefinition("com.blikk.crawler.frontier-x", "topic", true)
  val FrontierQueue = RabbitQueueDefinition("com.blikk.crawler.frontier-q", true, false, false, Map.empty)
  val FrontierScheduledQueue = RabbitQueueDefinition("com.blikk.crawler.frontier-scheduled-q", true,
    false, false, Map("x-dead-letter-exchange" -> FrontierExchange.name))

  /* Declares all commonly used RabbitMQ exchanges and queues */
  def declareAll()(implicit channel: RabbitChannel) : Unit = {
    // Data Exchange
    declareExchange(DataExchange)
    declareExchange(FrontierExchange)
    declareQueue(FrontierQueue)
    declareQueue(FrontierScheduledQueue)
  }

  def declareExchange(exchange: RabbitExchangeDefinition)(implicit channel: RabbitChannel) = {
    log.info("declaring RabbitMQ exchange=\"{}\"", exchange.name)
    channel.exchangeDeclare(exchange.name, exchange.exchangeType, exchange.durable) 
  }

  def declareQueue(queue: RabbitQueueDefinition)(implicit channel: RabbitChannel) = {
    log.info("declaring RabbitMQ queue=\"{}\"", queue.name)
    channel.queueDeclare(queue.name, queue.durable, queue.exclusive, 
      queue.autoDelete, queue.options)
  }

}