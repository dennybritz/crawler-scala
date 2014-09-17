package org.blikk.test

import org.blikk.crawler.client._
import org.blikk.crawler._
import akka.actor._
import akka.testkit._
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.config._
import org.apache.commons.lang3.SerializationUtils

class CrawlerClientSpec extends AkkaRemoteSpec("ApiClientSpec")  {

  val exchangeName = "blikk-test-exchange"
  val routingKey = "testApp"

  val apiAutoPilot = new TestActor.AutoPilot {
    def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
      msg match { case ConnectionInfoRequest => 
        sender ! ConnectionInfo(rabbitMQconnectionString)
        TestActor.NoAutoPilot
      }
  }

  before {
    addNode("system1", 9001)
    addNode("system1", 9002)
    withLocalRabbit { channel =>
      channel.exchangeDeclare(exchangeName, "direct", false)
    }
  }

  after { shutdown() }

  describe("Crawler Client") {
    
    it("should work") {
      implicit val mat = FlowMaterializer(MaterializerSettings(systems(1)))(systems(1))
      probes(0).setAutoPilot(apiAutoPilot)
      val client = new CrawlerClient(
        s"akka.tcp://system1@localhost:9001/system/${probes(0).ref.path.name}", 
        "testApp", exchangeName)(systems(1))
      val stream = client.createContext[String]()
      stream.flow.foreach(probes(1).ref ! _)
      publishMsg(SerializationUtils.serialize("message1"), exchangeName, routingKey)
      publishMsg(SerializationUtils.serialize("message2"), exchangeName, routingKey)    
      assert(probes(1).receiveN(2).toSet == Set("message1", "message2"))
    }

  }

}