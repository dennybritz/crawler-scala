package org.blikk.crawler

import com.rabbitmq.client.{Channel => RabbitChannel, ConnectionFactory => RabbitConnectionFactory}
import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit

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
  durable: Boolean,
  autoDelete: Boolean,
  internal: Boolean = false,
  arguments: Map[String, AnyRef] = Map.empty)

/** 
  * RabbitMQ data definitions shared by several classes 
  */
object RabbitData extends Logging {

  // Default configuration
  val config = ConfigFactory.load()
  val defaultRabbitMQUri : String = config.getString("blikk.rabbitMQ.uri")
  val requestHeartbeat : Int = config.getDuration("blikk.rabbitMQ.requestHeartbeat", TimeUnit.SECONDS).toInt

  // Initializes the RabbitMQ Connection Factory. 
  // An application should use one Connection with multiple channels.
  private val rabbitCF = new RabbitConnectionFactory()
  rabbitCF.setRequestedHeartbeat(requestHeartbeat)
  setRabbitUri(defaultRabbitMQUri)
  private lazy val rabbitConnection = rabbitCF.newConnection()
  

  /* Exchanges */
  /* ================================================== */

  // The RabbitMQ default exchange
  val DefaultExchange = RabbitExchangeDefinition("", "direct", true, false)

  // Used to exchange messages between crawl platform and crawl apps
  val DataExchange = RabbitExchangeDefinition("com.blikk.crawler.data-x", "direct", true, false)

  // Send data to the frontier queue
  val FrontierExchange = RabbitExchangeDefinition("com.blikk.crawler.frontier-x", "topic", true, false)

  
  /* Queues */
  /* ================================================== */

  def queueForApp(appId: String) = 
    RabbitQueueDefinition(s"${appId}", true, false, false, Map.empty)

  val FrontierQueue = RabbitQueueDefinition("com.blikk.crawler.frontier-q", true, false, false, Map.empty)
  val FrontierScheduledQueue = RabbitQueueDefinition("com.blikk.crawler.frontier-scheduled-q", true,
    false, false, Map("x-dead-letter-exchange" -> FrontierExchange.name))

  
  /* Helper Methods */
  /* ================================================== */

  def createChannel() : RabbitChannel = rabbitConnection.createChannel()

  /* Changes the default connection information. This should only be called before any connection is created */
  def setRabbitUri(rabbitMQUri: String) : Unit = {
    log.info("Using RabbitMQ connection URI: {}", rabbitMQUri)
    rabbitCF.setUri(rabbitMQUri)
  }

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
    channel.exchangeDeclare(exchange.name, exchange.exchangeType, exchange.durable, 
      exchange.autoDelete, exchange.internal, exchange.arguments) 
  }

  def declareQueue(queue: RabbitQueueDefinition)(implicit channel: RabbitChannel) = {
    log.info("declaring RabbitMQ queue=\"{}\"", queue.name)
    channel.queueDeclare(queue.name, queue.durable, queue.exclusive, 
      queue.autoDelete, queue.options)
  }

  def deleteExchange(exchange: RabbitExchangeDefinition)(implicit channel: RabbitChannel) = {
    log.info("deleting RabbitMQ exchange=\"{}\"", exchange.name)
    channel.exchangeDelete(exchange.name) 
  }

}