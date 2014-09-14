package org.blikk.crawler

import com.redis.RedisClientPool
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.routing._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing._
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._


object CrawlService {
  def props(localRedis: RedisClientPool) 
    = Props(classOf[CrawlService], localRedis)
}

class CrawlService(val localRedis: RedisClientPool) 
  extends CrawlServiceLike with Actor with ActorLogging {

  /* Consistent-hashing router that forwards requests to the appropriate crawl service node */
  val _serviceRouter : ActorRef = context.actorOf(ClusterRouterGroup(
    ConsistentHashingGroup(Nil), ClusterRouterGroupSettings(
      totalInstances = 100, routeesPaths = List("/user/crawlService"), 
      allowLocalRoutees = true, useRole = None)).props(),
    name="serviceRouter")
  def serviceRouter = _serviceRouter

  val peerScatterGatherRouter = context.actorOf(ClusterRouterGroup(
    ScatterGatherFirstCompletedGroup(Nil, 5.seconds), ClusterRouterGroupSettings(
      totalInstances = 100, routeesPaths = List("/user/crawlService"), 
      allowLocalRoutees = true, useRole = None)).props(), "peerScatterGatherRouter")


  val jobStatsCollector = context.actorOf(JobStatsCollector.props(localRedis), "jobStatsCollector")

  override def preStart() : Unit = {
    log.info(s"starting at ${self.path}")
    Cluster(context.system)
      .subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent])
  }

  val clusterBehavior : Receive = {
    case clusterEvent : MemberEvent =>
      log.info("Cluster event: {}", clusterEvent.toString)
  }
  
  def receive = clusterBehavior orElse defaultBehavior

}