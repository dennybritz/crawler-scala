package org.blikk.crawler

import com.rabbitmq.client.{Connection => RabbitMQConnection}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.routing._
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing._
import akka.stream.scaladsl2.ImplicitFlowMaterializer
import scala.concurrent.duration._


object CrawlService {
  def props(rabbitMQ: RabbitMQConnection) 
    = Props(classOf[CrawlService], rabbitMQ)
}

class CrawlService(implicit val rabbitMQ: RabbitMQConnection) 
  extends CrawlServiceLike with Actor with ActorLogging with ImplicitFlowMaterializer {

  val serviceRouter : ActorRef = context.actorOf(ClusterRouterGroup(
    ConsistentHashingGroup(Nil), ClusterRouterGroupSettings(
      totalInstances = 100, 
      routeesPaths = List("/user/crawlService"), 
      allowLocalRoutees = true, 
      useRole = None)).props(),
    "serviceRouter")

  val peerScatterGatherRouter = context.actorOf(ClusterRouterGroup(
    ScatterGatherFirstCompletedGroup(Nil, 5.seconds), 
    ClusterRouterGroupSettings(
      totalInstances = 100, 
      routeesPaths = List("/user/crawlService"), 
      allowLocalRoutees = true, 
      useRole = None)).props(), 
    "peerScatterGatherRouter")

  override def preStart() : Unit = {
    log.info(s"starting at ${self.path}")
    Cluster(context.system)
      .subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent])
    initializeSinks()
  }

  val clusterBehavior : Receive = {
    case clusterEvent : MemberEvent =>
      log.info("Cluster event: {}", clusterEvent.toString)
  }
  
  def receive = clusterBehavior orElse defaultBehavior

}