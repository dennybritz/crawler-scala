package org.blikk.crawler

import akka.actor._
import com.rabbitmq.client._

/** 
  * Consumes a RabbitMQ queue and sends all received data to a given actor
  */
class RabbitConsumer(channel: Channel, target: ActorRef)
  (implicit val system: ActorSystem) extends DefaultConsumer(channel) 
  with ImplicitLogging {

  override def handleDelivery(consumerTag: String, envelope: Envelope, 
    properties: AMQP.BasicProperties, body: Array[Byte]) : Unit = {
    //log.debug(envelope.getDeliveryTag)
    val msg = RabbitMessage(
      envelope.getRoutingKey, 
      properties.getContentType, 
      envelope.getDeliveryTag, 
      body) 
    target ! msg
  }
                                  
}