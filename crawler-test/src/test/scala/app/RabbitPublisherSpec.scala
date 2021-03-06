package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._

class RabbitPublisherSpec extends AkkaSingleNodeSpec("RabbitPublisherSpec")
  with LocalRabbitMQ  {

    val exchange = RabbitExchangeDefinition(s"${this.name}-exchange", "direct", false, true)
    val queue = RabbitQueueDefinition(s"${this.name}-queue", false)
    val routingKey = "*"

    import system.dispatcher

    before { withLocalRabbit { implicit channel =>
      RabbitData.declareExchange(exchange)
    }}

    describe("The RabbitPublisher") {
      it("should work") {
        withLocalRabbit { channel =>
          // Get a flow
          val actor = system.actorOf(RabbitPublisher.props(channel, 
            queue, exchange, routingKey))
          implicit val m = FlowMaterializer(MaterializerSettings(system))
          val flow = Source(ActorPublisher[Any](actor))
          flow.map { x => 
            new String(x.asInstanceOf[Array[Byte]]) 
          }.foreach(self ! _)
          publishMsg("message1".getBytes, exchange.name, routingKey)
          publishMsg("message2".getBytes, exchange.name, routingKey)
          // Start consuming the queue
          assert(receiveN(2).toSet == Set("message1", "message2"))
          expectNoMsg()
        }
      }
    }

  }
