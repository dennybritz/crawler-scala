package org.blikk.test

import org.scalatest._
import org.blikk.crawler.{JobConfiguration, ResponseProcessorInput}
import org.blikk.crawler.processors._
import org.blikk.crawler.channels._
import com.typesafe.config.ConfigFactory

class RabbitMQChannelSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll {

  // TODO: Put this into the configuration
  val connectionString = "amqp://guest:guest@localhost:5672"

  describe("RabbitMQ I/O") {

    it("should work") {
      val p = new RabbitMQProducer("testRabbitMQProducer", connectionString, "blikkTestQueue")
      val result = p.process(ResponseProcessorInput(
        new spray.http.HttpResponse(entity=spray.http.HttpEntity("Hello!")), 
        new spray.http.HttpRequest,
        JobConfiguration.empty("test"), Map.empty))
      val c = new RabbitMQChannel
      c.pipe(result("testRabbitMQProducer").asInstanceOf[RabbitMQChannelInput])

      // TODO: Make sure the value actually ends up in the queue
      // (I checked it manually)
    }

  }

}