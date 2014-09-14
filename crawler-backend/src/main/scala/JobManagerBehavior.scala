package org.blikk.crawler

import akka.actor._
import akka.pattern.{pipe, ask}
import akka.routing.{GetRoutees, Routees, ActorRefRoutee, ActorSelectionRoutee, Broadcast}
import scala.concurrent.{Future}

// import akka.util.Timeout
// import scala.concurrent.duration._

trait JobManagerBehavior { this: Actor with ActorLogging with CrawlServiceLike =>

  // implicit val askTimeout = Timeout(5.seconds)
  import context.dispatcher

  def jobManagerBehavior : Receive = {
    case msg @ GetJobEventCounts(jobId: String) =>
      jobStatsCollector.tell(msg, sender())
    case msg @ Broadcast(StopJob(jobId: String)) =>
      serviceRouter ! msg
    case StopJob(jobId: String) =>
      log.info("stopping job=\"{}\"",jobId)
      // TODO: This results in requests still being routed, but the worker won't find the jobConf
      // The worker will then drop the request. It's inefficient because it results in excess messages
      jobCache.remove(jobId)
      frontiers.get(jobId).foreach(_ ! StopFrontier)
    case DestroyJob(jobId: String) =>
      log.info("destroying job=\"{}\"",jobId)
      frontiers.get(jobId).foreach(_ ! ClearFrontier)
      frontiers.get(jobId).foreach(context.stop)
      jobStatsCollector ! ClearJobEventCounts(jobId)
      sender ! "ok"
  }

}