package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.stream.actor._
import org.blikk.crawler.processors.RabbitMQSink
import org.blikk.crawler._
import org.scalatest._
import scala.concurrent.duration._

class RabbitMQSinkSpec extends AkkaSingleNodeSpec("RabbitMQSinkSpec") {

  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  val exchangeDef = RabbitExchangeDefinition("RabbitMQSinkSpec-ex", "fanout", false)
  val queueDef = RabbitQueueDefinition("", false)

  describe("RabbitMQ Sink") {
    
    it("should write items to Rabbit"){
      val data = List("Are", "you", "ready?")
      val rabbitSink = RabbitMQSink.build[String](
        RabbitData.createChannel(), exchangeDef)(x => (x.getBytes, "*"))

      // To make sure the data is in RabbitMQ we use our RabbitPublisher to receive the data
      val publisherActor = system.actorOf(
        RabbitPublisher.props(RabbitData.createChannel(), queueDef, exchangeDef, "*"), 
        "publisher")
      val publisherInput = ActorPublisher[Array[Byte]](publisherActor)
      
      // Give RabbitMQ a little bit of time to initialize the consumers
      Thread.sleep(100)

      // Create the flow
      Source(publisherInput).to(Sink.foreach[Array[Byte]] { item =>
        val msgStr = new String(item)
        log.debug(msgStr)
        self ! msgStr
      }).run()

      // Run the flow that writes to RabbitMQ
      val flow = Source(data).to(rabbitSink).run()
      receiveN(3).map(_.toString).toSet shouldBe Set("Are", "you", "ready?")
      expectNoMsg()
    }

  }

}