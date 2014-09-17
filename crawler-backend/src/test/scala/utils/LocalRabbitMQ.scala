package org.blikk.test

import com.rabbitmq.client._
import org.blikk.crawler.Resource

trait LocalRabbitMQ {

  val rabbitMQconnectionString = "amqp://guest:guest@localhost:5672"
  val factory = new ConnectionFactory()
  factory.setUri(rabbitMQconnectionString)

  def withLocalRabbit[A](func: Channel => A) : A = {
    val conn = factory.newConnection()
    val channel = conn.createChannel()
    Resource.using(conn) { conn =>
      Resource.using(channel) { channel =>
        func(channel)
      }
    }
  }

  def purgeQueue(queueName: String) : Unit = {
    withLocalRabbit { channel =>
      scala.util.Try(channel.queueDeclarePassive(queueName))
      channel.queuePurge(queueName)
    } 
  }

  def publishMsg(msg: Array[Byte], exchangeName: String, routingKey: String = "") = {
    // Very inefficient, but it's for testing only!
    withLocalRabbit { channel =>
      channel.basicPublish(exchangeName, routingKey, null, msg)
    }
  }

}