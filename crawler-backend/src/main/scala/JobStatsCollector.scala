package org.blikk.crawler

import com.redis.RedisClient
import akka.actor._
import scala.collection.mutable.{Map => MutableMap}

object JobStatsCollector {
  def props(localRedis: RedisClient) = Props(classOf[JobStatsCollector], localRedis)
}

/* 
  Collects statistics about Jobs.
  Note: This runs locally on each node and only collects local job statistics.
  To obtain global job statistics across all nodes one must aggregate all local statistics. 
*/
class JobStatsCollector(localRedis: RedisClient) extends Actor with ActorLogging {

  val events = MutableMap[(String, String), Int]().withDefaultValue(0)

  def key(jobId: String, eventName: String) = s"stats:${jobId}:${eventName}"
  def eventKeys(jobId: String) = s"stats:${jobId}:events"

  override def preStart(){
    log.info("started")
    // Subscribe to the system event stream for job event
    context.system.eventStream.subscribe(self, classOf[JobEvent])
  }

  def receive = {
    case e @ JobEvent(jobId, event) =>
      log.info(e.toString)
      increaseEventCounts(jobId, processJobEvent(e))
    case GetJobEventCount(jobId, event) =>
      sender ! JobStats(jobId, Map(event.toString -> getEventCount(jobId, event.toString)))
    case GetJobEventCounts(jobId) =>
      // Return all events counts for this job but strip off the jobId from the result
      sender ! JobStats(jobId, getAllEventCounts(jobId))
    case ClearJobEventCounts(jobId) =>
      clearEventCounts(jobId)
  }

  def clearEventCounts(jobId: String) : Unit = {
    localRedis.smembers(eventKeys(jobId)).foreach { keys =>
      keys.flatten.foreach { eventName =>
        localRedis.del(key(jobId, eventName))
      }
    }
    localRedis.del(eventKeys(jobId))
  } 

  def getAllEventCounts(jobId: String) : Map[String, Int] = {
    localRedis.smembers(eventKeys(jobId)).map { keys =>
      keys.flatMap { 
        case Some(eventName) => Some(eventName, getEventCount(jobId, eventName))
        case _ => None
      }.toMap
    }.getOrElse(Map.empty)
  }

  def getEventCount(jobId: String, eventName: String) : Int = {
    localRedis.get(key(jobId, eventName)).map(_.toInt).getOrElse(0)
  }

  def increaseEventCounts(jobId: String, events: List[String]) ={
    localRedis.sadd(eventKeys(jobId), events.head, events.tail: _*)
    events.foreach { e => 
      localRedis.incr(key(jobId, e))
    }
  }

  def processJobEvent(e: JobEvent) : List[String] = {
    e.event match {
      case req : WrappedHttpRequest =>
        List("requests", s"${req.host}:requests")
      case FetchResponse(res, req, jobId) =>
        List("responses", s"${req.host}:responses")
      case str : String => 
        List(str)
      case other => 
        log.warning("unhandled JobEvent type: {}", other)
        List.empty
    }
  }



}