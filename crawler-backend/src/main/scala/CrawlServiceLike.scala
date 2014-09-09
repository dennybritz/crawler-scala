package org.blikk.crawler

import com.redis.RedisClientPool
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.routing.{Broadcast, FromConfig, BalancingPool, GetRoutees, Routees, ActorRefRoutee, ActorSelectionRoutee}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.util.Timeout
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap

trait CrawlServiceLike extends JobManagerBehavior { this: Actor with ActorLogging =>

  val NumWorkers = 50
  
  implicit val askTimeout = Timeout(5.seconds)
  import context.dispatcher

  /* Local redis instance used for caching */
  implicit def localRedis: RedisClientPool

  /* The balancing router distributed work across all workers */
  lazy val workerPool = context.actorOf(
    BalancingPool(NumWorkers).props(HostWorker.props(self)), "balancingPool")

  /* Keeps track of all jobs */
  val jobCache = MutableMap[String, JobConfiguration]()
  /* Keeps track of frontiers for different jobs */
  val frontiers = MutableMap[String, ActorRef]()

  /* Routes request globally across the cluster */
  def serviceRouter : ActorRef 
  /* Collects job statistics */
  def jobStatsCollector : ActorRef

  def crawlServiceBehavior : Receive = {
    case RouteFetchRequest(fetchReq) => 
      log.debug("routing fetch request {}", fetchReq.req)
      routeFetchRequestGlobally(fetchReq)
    case msg @ FetchRequest(req, jobId) =>
      routeFetchRequestLocally(msg, sender())
    case GetJob(jobId) =>
      sender ! jobCache.get(jobId)
    case RegisterJob(job, clearOldJob) =>
      log.info("registering job=\"{}\"", job.jobId)
      if(clearOldJob) {
        jobStatsCollector ! ClearJobEventCounts(job.jobId)
      }
      jobCache.put(job.jobId, job)
      startFrontier(job.jobId)
    case RunJob(job, clearOldJob) =>
      // Store the job configuration locally and send it to all workers for caching
      log.debug("broadcasting new job=\"{}\"", job.jobId)
      serviceRouter ! Broadcast(RegisterJob(job, clearOldJob))
      // Send out the initial requests to appropriate workers
      job.seeds.foreach { seedRequest =>
        routeFetchRequestGlobally(FetchRequest(seedRequest, job.jobId))
      }
    case msg @ AddToFrontier(req, jobId) => 
      frontiers.get(jobId) match {
        case Some(frontier) => frontier ! msg
        case None => log.warning("""no frontier running for job="{}" """, jobId)
      }
  }

  def defaultBehavior : Receive = crawlServiceBehavior orElse jobManagerBehavior

  /* Routes a fetch request using consistent hasing to the right cluster node */
  def routeFetchRequestGlobally(fetchRequest: FetchRequest) : Unit = {
    serviceRouter ! ConsistentHashableEnvelope(
      AddToFrontier(fetchRequest.req, fetchRequest.jobId), fetchRequest.req.host)
  }

  /* 
    Forwards the fetch request to the worker responsible for the host.
    Starts a new worker if no responsible worker exists yet.
  */
  def routeFetchRequestLocally(req: FetchRequest, sender: ActorRef) : Unit = {
    // Router does the managing for us
    workerPool.tell(req, sender)
  }

  /* Starts a new frontier worker for a given job */
  def startFrontier(jobId: String) : Unit = {
    val newFrontierActor = context.actorOf(Frontier.props(jobId, localRedis), s"frontier-${jobId}")
    context.watch(newFrontierActor)
    newFrontierActor ! ClearFrontier
    newFrontierActor ! StartFrontier(1.seconds, self)
    frontiers.put(jobId, newFrontierActor)
  }

}