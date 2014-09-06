package org.blikk.crawler

import akka.pattern.{pipe, ask}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.routing.{Broadcast, FromConfig}
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.concurrent.Future

trait CrawlServiceLike { this: Actor with ActorLogging =>

  /* Keeps track of which worker is responsible for which host */
  val hostWorkers = MutableMap[String, ActorRef]()
  /* Keeps track of all jobs */
  val jobCache = MutableMap[String, JobConfiguration]()

  def serviceRouter : ActorRef 

  def crawlServiceBehavior : Receive = {
    case msg @ FetchRequest(req, jobId) =>
      // We assume that the request comes from the consistent hasing router
      // and that this node is in fact responsible for handling it.
      val res = routeFetchRequestLocally(msg, sender())
    case GetJob(jobId) =>
      sender ! jobCache.get(jobId)
    case RegisterJob(job) =>
      log.info("registering job=\"{}\"", job.jobId)
      jobCache.put(job.jobId, job)
    case RunJob(job) =>
      // Store the job configuration locally and send it to all workers for caching
      log.debug("broadcasting new job=\"{}\"", job.jobId)
      serviceRouter ! Broadcast(RegisterJob(job))
      // Send out the initial requests to appropriate workers
      job.seeds.foreach { seedRequest =>
        val fetchReq = FetchRequest(seedRequest, job.jobId)
        routeFetchRequest(fetchReq)
      }
  }

  def routeFetchRequest(fetchRequest: FetchRequest) : Unit = {
    serviceRouter ! fetchRequest
  }

  /* 
    Forwards the fetch request to the worker responsible for the host.
    Starts a new worker if no responsible worker exists yet.
  */
  def routeFetchRequestLocally(req: FetchRequest, sender: ActorRef) : Unit = {
    val host = req.req.host
    log.debug(s"locally routing fetch request for host={}", host)
    hostWorkers.get(host) match {
      case Some(worker) => worker.tell(req, sender) 
      case None => 
        val newWorker = startWorkerForHost(host)
        hostWorkers.put(host, newWorker)
        newWorker.tell(req, sender)
    }
  }

  /* Starts a new worker actor for a given host */
  def startWorkerForHost(host: String) : ActorRef = {
    log.debug(s"starting worker for host={}", host)
    val newWorker = context.actorOf(HostWorker.props(host), s"hostWorker-${host}")
    newWorker
  }

}