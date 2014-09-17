package org.blikk.test

import org.blikk.crawler.app._
import org.blikk.crawler._
import akka.actor._
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._

class RabbitPublisherSpec extends AkkaSingleNodeSpec("RabbitPublisherSpec")
  with LocalRabbitMQ  {

    val log = akka.event.Logging.getLogger(system, this)
    val exchangeName = "blikk-test-exchange"
    val queueName = ""
    val routingKey = "testKey"

    import system.dispatcher

    describe("The RabbitPublisher") {
      it("should work") {
        withLocalRabbit { channel =>
          // Get a flow
          val actor = system.actorOf(RabbitPublisher.props(channel, queueName, 
            exchangeName, routingKey))
          implicit val m = FlowMaterializer(MaterializerSettings(system))
          val flow = Flow(ActorPublisher[Any](actor))
          flow.map { x => 
            new String(x.asInstanceOf[Array[Byte]]) 
          }.foreach(self ! _)
          publishMsg("message1".getBytes, exchangeName, routingKey)
          publishMsg("message2".getBytes, exchangeName, routingKey)
          // Start consuming the queue
          assert(receiveN(2).toSet == Set("message1", "message2"))
          expectNoMsg()
        }
      }
    }

  }
