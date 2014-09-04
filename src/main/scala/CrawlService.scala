package org.blikk.crawler

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.{FromConfig, ConsistentHashingGroup, AddRoutee, ActorRefRoutee, Broadcast}
import akka.cluster.routing._
import scala.concurrent.duration._

class CrawlService extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  val serviceRouter = context.actorOf(Props.empty.withRouter(FromConfig),
    name = "serviceRouter")

  import context.dispatcher

  override def preStart(): Unit = {
    log.info(s"Starting at ${self.path}...")
    // Subscribe to cluster changes
    // Receive the initial state of the cluster as separate events
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    }
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case x => log.info(x.toString)

  }

}