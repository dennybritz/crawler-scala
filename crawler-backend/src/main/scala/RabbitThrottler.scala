package org.blikk.crawler

import akka.actor._
import scala.concurrent.duration._

object RabbitThrottler {

  case class AddSchedule(routingKey: String, initialDelay: FiniteDuration, interval: FiniteDuration)
  case class RemoveSchedule(routingKey: String)
  case class DequeueItem(routingKey: String)

}

trait RabbitThrottler extends Actor with ActorLogging {

  import RabbitThrottler._
  import context.dispatcher

  /* You must implement this method */  
  def handleItem(item: RabbitMessage) : Unit

  def rabbitExchange : RabbitExchangeDefinition
  def bindRoutingKey : String
  var cancelTokens = Map[String, Cancellable]()
  var rabbitQueues = Map[String, String]()

  implicit lazy val rabbitChannel = RabbitData.createChannel()

  def receive = {
    case AddSchedule(routingKey, initialDelay, interval) => 
      addSchedule(routingKey, initialDelay, interval)
    case RemoveSchedule(routingKey) =>
      removeSchedule(routingKey)
    case DequeueItem(routingKey) =>
      dequeueItem(routingKey)
  }

  override def postStop(){
    // Remove all schedules
    cancelTokens.values.foreach(_.cancel())
    log.info("Shutdown: Cancelled all schedules.")
    // Unbind all queues
    rabbitQueues.foreach { case(routingKey, queueName) =>
      rabbitChannel.queueUnbind(queueName, rabbitExchange.name, routingKey)
    }
    log.info("Shutdown: Unbound all queues.")
    // Close the channel we created
    rabbitChannel.close()
  }

  def addSchedule(routingKey: String, initialDelay: FiniteDuration, interval: FiniteDuration) : Unit = {
    // Make sure the timer doesn't already exist
    if (cancelTokens.contains(routingKey)){
      log.warning("Schedule for routingKey='{}' already exists, not creating.", routingKey)
      return
    }

    // Reuse an existing queue if possible. If not, create a new one.
    // TODO: The queues should be persisted. Use Akka's persistent actors?
    val queueName = rabbitQueues.get(routingKey).getOrElse {
      val newQueue = rabbitChannel.queueDeclare().getQueue()
      rabbitQueues += Tuple2(routingKey, newQueue)
      newQueue
    }
    rabbitChannel.queueBind(queueName, rabbitExchange.name, routingKey)
    log.info("Binding routingKey='{}' queueName='{}' exchangeName='{}'", routingKey, queueName, rabbitExchange.name)

    // Add to the scheduler
    val cancelToken = context.system.scheduler.schedule(initialDelay, interval) {
      self ! DequeueItem(routingKey)
    }
    cancelTokens += Tuple2(routingKey, cancelToken)
    log.info("Added schedule for routingKey='{}'", routingKey)

    // Say OK
    sender ! queueName
  }

  def removeSchedule(routingKey: String) : Unit = {
    cancelTokens.get(routingKey) match {
      case Some(token) => 
        token.cancel()
        cancelTokens -= routingKey
        log.info("Cancelled timer for routingKey='{}'", routingKey)
      case None =>
        log.error("No timer for routingKey='{}' found. Not cancelling.", routingKey)
    }
  }

  def dequeueItem(routingKey: String) : Unit = {
    val queueName = rabbitQueues.get(routingKey) getOrElse {
      log.error("Could not find queue for routingKey={}", routingKey)
      self ! RemoveSchedule(routingKey)
      return
    }

    Option(rabbitChannel.basicGet(queueName, true)).map { response =>
      RabbitMessage(
        response.getEnvelope.getRoutingKey, 
        response.getProps.getContentType, 
        response.getEnvelope.getDeliveryTag, 
        response.getBody)
    }.foreach(handleItem)
  }
}