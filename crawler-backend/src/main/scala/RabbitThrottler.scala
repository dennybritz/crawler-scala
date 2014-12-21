package org.blikk.crawler

import akka.actor._
import scala.concurrent.duration._

object RabbitThrottler {

  case class AddSchedule(routingKey: String, initialDelay: FiniteDuration, interval: FiniteDuration)
  case class RemoveSchedule(routingKey: String)
  case class DequeueItem(routingKey: String)
  case class AddQueue(routingKey: String, queueProperties: RabbitQueueDefinition)

}

trait RabbitThrottler extends Actor with ActorLogging {

  import RabbitThrottler._
  import context.dispatcher

  // Needs implementation: What happens to dequeued items?
  def handleItem(routingKey: String, item: RabbitMessage) : Unit
  // Needs implementation: Called when no item was in RabbitMQ
  // Can be used to time out schedules
  def handleNoItem(routingKey: String) : Unit

  // Needs implementation: The exchange we bind our queues to
  def rabbitExchange : RabbitExchangeDefinition
  
  var schedules = Map[String, Cancellable]()
  var rabbitQueues = Map[String, String]()

  lazy val rabbitChannel = RabbitData.createChannel()

  def rabbitThrottlerBehavior : Receive = {
    case AddSchedule(routingKey, initialDelay, interval) => 
      addSchedule(routingKey, initialDelay, interval)
      sender ! "OK"
    case RemoveSchedule(routingKey) =>
      removeSchedule(routingKey)
    case DequeueItem(routingKey) =>
      dequeueItem(routingKey)
    case AddQueue(routingKey, queueProperties) =>
      sender ! addQueue(routingKey, queueProperties)
  }

  override def postStop(){
    // Remove all schedules
    schedules.values.foreach(_.cancel())
    log.info("Shutdown: Cancelled all schedules.")
    // Unbind all queues
    rabbitQueues.foreach { case(routingKey, queueName) =>
      rabbitChannel.queueUnbind(queueName, rabbitExchange.name, routingKey)
    }
    log.info("Shutdown: Unbound all queues.")
    // Close the channel we created
    rabbitChannel.close()
  }

  def addSchedule(routingKey: String, initialDelay: FiniteDuration, interval: FiniteDuration) : Cancellable = {
    schedules.get(routingKey) getOrElse {
      val cancelToken = context.system.scheduler.schedule(initialDelay, interval) { self ! DequeueItem(routingKey) }
      schedules += Tuple2(routingKey, cancelToken)
      log.info("Added schedule for routingKey='{}'", routingKey)
      cancelToken
    }    
  }

  def removeSchedule(routingKey: String) : Unit = {
    schedules.get(routingKey) match {
      case Some(token) => 
        token.cancel()
        schedules -= routingKey
        log.info("Cancelled timer for routingKey='{}'", routingKey)
      case None =>
        log.error("No timer for routingKey='{}' found. Not cancelling.", routingKey)
    }
  }

  def addQueue(routingKey: String, queueProperties: RabbitQueueDefinition) : String = {
    // Reuse an existing queue if possible. If not, create a new one.
    // TODO: The queues should be persisted. Use Akka's persistent actors?
    rabbitQueues.get(routingKey).getOrElse {
      implicit val channel = RabbitData.createChannel()
      val newQueueName = RabbitData.declareQueue(queueProperties).getQueue()
      channel.queueBind(newQueueName, rabbitExchange.name, routingKey)
      channel.close()
      rabbitQueues += Tuple2(routingKey, newQueueName)
      log.info("Bound routingKey='{}' queueName='{}' exchangeName='{}'", 
        routingKey, newQueueName, rabbitExchange.name)
      newQueueName
    }
  }

  def dequeueItem(routingKey: String) : Unit = {
    val queueName = rabbitQueues.get(routingKey) getOrElse {
      log.error("Could not find queue for routingKey={}", routingKey)
      self ! RemoveSchedule(routingKey)
      return
    }

    Option(rabbitChannel.basicGet(queueName, false)).map { response =>
      RabbitMessage(
        response.getEnvelope.getRoutingKey, 
        response.getProps.getContentType, 
        response.getEnvelope.getDeliveryTag, 
        response.getBody)
    } match {
      case Some(item) => handleItem(routingKey, item)
      case None => handleNoItem(routingKey)
    }
  }
}