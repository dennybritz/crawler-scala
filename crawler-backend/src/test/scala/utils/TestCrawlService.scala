package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.routing._
import akka.testkit._
import com.redis.RedisClientPool

object TestCrawlService {
  def props(implicit localRedis: RedisClientPool, redisPrefix: String = "") 
    = Props(classOf[TestCrawlService], localRedis, redisPrefix)
}

class TestCrawlService(val localRedis: RedisClientPool, val redisPrefix: String) 
  extends CrawlServiceLike with Actor with ActorLogging {
  
  val _serviceRouter = context.actorOf(ConsistentHashingGroup(
    List(self.path.toStringWithoutAddress)).props(), 
    "serviceRouter")
  override val serviceRouter = _serviceRouter

  val jobStatsCollector = context.actorOf(JobStatsCollector.props(localRedis), "jobStatsCollector")

  def extraBehavior : Receive = {
    case msg : AddRoutee =>
      _serviceRouter ! msg
  }

  def receive = extraBehavior orElse defaultBehavior
 

}
