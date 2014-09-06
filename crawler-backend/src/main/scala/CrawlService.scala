package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.routing._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.{Broadcast, FromConfig, ConsistentHashingGroup}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._

class CrawlService extends CrawlServiceLike with Actor with ActorLogging {

  /* Consistent-hashing router that forwards requests to the appropriate crawl service node */
  val _serviceRouter : ActorRef = context.actorOf(ClusterRouterGroup(
    ConsistentHashingGroup(Nil), ClusterRouterGroupSettings(
      totalInstances = 100, routeesPaths = List("/user/crawlService"), 
      allowLocalRoutees = true, useRole = None)).props(),
    name="serviceRouter")
  def serviceRouter = _serviceRouter

  override def preStart() : Unit = {
    log.info(s"starting at ${self.path}")
    Cluster(context.system)
      .subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent])
  }
  
  def receive = crawlServiceBehavior orElse {
    case clusterEvent : MemberEvent =>
      log.debug("Cluster event: {}", clusterEvent.toString)
  }

  override def routeFetchRequest(fetchRequest: FetchRequest) : Unit = {
    serviceRouter ! ConsistentHashableEnvelope(fetchRequest, fetchRequest.req.host)
  }

}