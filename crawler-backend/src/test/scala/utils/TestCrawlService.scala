package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.routing._
import akka.testkit._
import com.redis.RedisClientPool
import scala.concurrent.duration._

object TestCrawlService {
  def props(implicit localRedis: RedisClientPool) 
    = Props(classOf[TestCrawlService], localRedis)
}

class TestCrawlService(val localRedis: RedisClientPool) 
  extends CrawlServiceLike with Actor with ActorLogging {
  
  lazy val serviceRouter = context.actorOf(ConsistentHashingGroup(
    List(self.path.toStringWithoutAddress)).props(), 
    "serviceRouter")
  lazy val peerScatterGatherRouter = context.actorOf(ScatterGatherFirstCompletedGroup(
    Nil, 5.seconds).props(), "peerScatterGatherRouter")

  val jobStatsCollector = context.actorOf(JobStatsCollector.props(localRedis), "jobStatsCollector")

  def extraBehavior : Receive = {
    case msg : AddRoutee =>
      serviceRouter ! msg
      peerScatterGatherRouter ! msg
  }

  def receive = extraBehavior orElse defaultBehavior
 
}
