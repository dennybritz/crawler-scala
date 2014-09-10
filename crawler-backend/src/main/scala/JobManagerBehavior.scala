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
     case GetGlobalJobStats(jobId) =>
      // Request local stats from each routee
      val _sender = sender()
      val jobStatsFuture = for {
        routees <- (serviceRouter ? GetRoutees).mapTo[Routees].map(_.routees)
        routeeRefs = routees.map {
          case r : ActorSelectionRoutee => r.selection
          case r : ActorRefRoutee => context.actorSelection(r.ref.path)
        }
        jobStatFutures = routeeRefs.map(r => (r ? GetJobEventCounts(jobId)).mapTo[JobStats])
        jobStats <- Future.sequence(jobStatFutures).mapTo[Seq[JobStats]]
      } yield jobStats
      // Aggregate all the local satistics
      val globalStatsFuture = jobStatsFuture.map { statsList =>
        val mergedCounts = statsList.map(_.eventCounts).reduce(_ ++ _).groupBy(_._1)
          .mapValues(_.map(_._2).sum)
        JobStats(jobId, mergedCounts)
      }
      // Send the result
      globalStatsFuture pipeTo _sender
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
  }

}