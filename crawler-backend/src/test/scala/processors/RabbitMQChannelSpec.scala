package org.blikk.test

import org.scalatest._
import org.blikk.crawler.{JobConfiguration, ResponseProcessorInput}
import org.blikk.crawler.processors._
import org.blikk.crawler.channels._
import com.typesafe.config.ConfigFactory

class RabbitMQChannelSpec extends FunSpec with BeforeAndAfter 
  with BeforeAndAfterAll with LocalRabbitMQ {

  // TODO: Put this into the configuration
  val queueName = "blikkTestQueue"

  before {
    withLocalRabbit { channel =>
      channel.queueDeclare(queueName, true, false, false, null)
      channel.queueDelete(queueName)
    }
  }

  describe("RabbitMQ I/O") {

    it("should work") {
      val p = new RabbitMQProducer("testRabbitMQProducer", rabbitMQconnectionString, queueName)
      val result = p.process(ResponseProcessorInput(
        new spray.http.HttpResponse(entity=spray.http.HttpEntity("Hello!")), 
        new spray.http.HttpRequest,
        JobConfiguration.empty("test"), Map.empty))
      val c = new RabbitMQChannel
      c.pipe(result("testRabbitMQProducer").asInstanceOf[RabbitMQChannelInput], 
        JobConfiguration.empty("test"), Map.empty)

      withLocalRabbit { channel =>
        channel.queueDeclare(queueName, true, false, false, null)
        val msg = channel.basicGet(queueName, true)
        assert(new String(msg.getBody()) === "Hello!")
      }
    }

  }

}