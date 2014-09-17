package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.routing._
import akka.testkit._
import com.redis.RedisClientPool
import scala.concurrent.duration._
import com.rabbitmq.client.{Connection => RabbitMQConnection}

object TestCrawlService {
  case class SetTarget(target: ActorRef)
  def props(rabbitMQ: RabbitMQConnection)
    = Props(classOf[TestCrawlService], rabbitMQ)
}

class TestCrawlService(val rabbitMQ: RabbitMQConnection) 
  extends CrawlServiceLike with Actor with ActorLogging {
    
  var target : ActorRef = self

  lazy val serviceRouter = context.actorOf(ConsistentHashingGroup(
    List(self.path.toStringWithoutAddress)).props(), 
    "serviceRouter")
  lazy val peerScatterGatherRouter = context.actorOf(ScatterGatherFirstCompletedGroup(
    Nil, 5.seconds).props(), "peerScatterGatherRouter")

  def extraBehavior : Receive = {
    case TestCrawlService.SetTarget(ref) =>
      target = ref
    case msg : AddRoutee =>
      serviceRouter ! msg
      peerScatterGatherRouter ! msg
  }

  def receive = extraBehavior orElse defaultBehavior

 
}
