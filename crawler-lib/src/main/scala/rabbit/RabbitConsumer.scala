package org.blikk.crawler

import akka.actor._
import com.rabbitmq.client._
import org.blikk.crawler._

class RabbitConsumer(channel: Channel, target: ActorRef)
  (implicit system: ActorSystem) extends DefaultConsumer(channel) {
  
  lazy val log = akka.event.Logging.getLogger(system, this)

  override def handleDelivery(consumerTag: String, envelope: Envelope, 
    properties: AMQP.BasicProperties, body: Array[Byte]) : Unit = {
    val msg = RabbitMessage(envelope.getRoutingKey, properties.getContentType, 
      envelope.getDeliveryTag, body) 
    target ! msg
  }
                                  
}