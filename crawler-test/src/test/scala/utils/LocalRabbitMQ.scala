package org.blikk.test

import com.rabbitmq.client._
import org.blikk.crawler.{Resource, Frontier}
import scala.util.Try

trait LocalRabbitMQ {

  val rabbitMQconnectionString = TestConfig.RabbitMQUri
  val factory = new ConnectionFactory()
  factory.setUri(rabbitMQconnectionString)

  /* Executes the block within a new connection and channel */
  def withLocalRabbit[A](func: Channel => A) : A = {
    val conn = factory.newConnection()
    val channel = conn.createChannel()
    Resource.using(conn) { conn =>
      Resource.using(channel) { channel =>
        func(channel)
      }
    }
  }

  /* Deletes queue data from RabbitMQ */
  def clearRabbitMQ(){
    List(Frontier.FrontierQueue.name,
      Frontier.FrontierScheduledQueue.name).foreach(deleteQueue)
  }

  def deleteQueue(queueName: String){
    val conn = factory.newConnection()
    val channel = conn.createChannel()
    val result = Try {
      channel.queueDeclarePassive(queueName)
      channel.queuePurge(queueName)
    }
    if (channel.isOpen) channel.close()
  }

  /* Publishes a message to RAbbitMQ */
  def publishMsg(msg: Array[Byte], exchangeName: String, routingKey: String = "") : Unit = {
    // Very inefficient, but it's for testing only!
    withLocalRabbit { channel =>
      channel.basicPublish(exchangeName, routingKey, null, msg)
    }
  }

}