package org.blikk.test

import com.rabbitmq.client._
import org.blikk.crawler.Resource

trait LocalRabbitMQ {

  val rabbitMQconnectionString = "amqp://guest:guest@localhost:5672"

  def withLocalRabbit[A](func: Channel => A) : A = {
    val factory = new ConnectionFactory()
    factory.setUri(rabbitMQconnectionString)
    val conn = factory.newConnection()
    val channel = conn.createChannel()
    Resource.using(conn) { conn =>
      Resource.using(channel) { channel =>
        func(channel)
      }
    }
  }

}