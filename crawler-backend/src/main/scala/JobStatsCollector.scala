package org.blikk.crawler

import com.redis.RedisClientPool
import akka.actor._
import scala.collection.mutable.{Map => MutableMap}
import com.redis.serialization.Parse

object JobStatsCollector {
  def props(localRedis: RedisClientPool) = Props(classOf[JobStatsCollector], localRedis)
}

/* 
  Collects statistics about Jobs.
  Note: This runs locally on each node and only collects local job statistics.
  To obtain global job statistics across all nodes one must aggregate all local statistics. 
*/
class JobStatsCollector(localRedis: RedisClientPool) extends Actor with ActorLogging {

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
      sender ! JobStats(jobId, getEventCounts(jobId, List(event.toString)))
    case GetJobEventCounts(jobId) =>
      // Return all events counts for this job but strip off the jobId from the result
      sender ! JobStats(jobId, getAllEventCounts(jobId))
    case ClearJobEventCounts(jobId) =>
      clearEventCounts(jobId)
  }

  def clearEventCounts(jobId: String) : Unit = {
    localRedis.withClient { client =>
      client.smembers(eventKeys(jobId)).foreach { keys =>
        keys.flatten.foreach { eventName =>
          client.del(key(jobId, eventName))
        }
      }
      client.del(eventKeys(jobId))
    }
  } 

  def getAllEventCounts(jobId: String) : Map[String, Int] = {
    localRedis.withClient { client =>
      client.smembers(eventKeys(jobId)).map { keys =>
        getEventCounts(jobId, keys.flatten.toList)
      }.getOrElse(Map.empty)
    }
  }

  def getEventCounts(jobId: String, eventNames: List[String]) : Map[String,Int] = {
    localRedis.withClient { client =>
      if (eventNames.isEmpty) return Map.empty 
      val keys = eventNames.map(e => key(jobId, e))
      import Parse.Implicits.parseInt
      client.mget[Int](keys.head, keys.tail: _*).map { values =>
        keys.zip(values.flatten).toMap
      }.getOrElse(Map.empty)
    }
  }

  def increaseEventCounts(jobId: String, events: List[String]) ={
    localRedis.withClient { client =>
      client.sadd(eventKeys(jobId), events.head, events.tail: _*)
      events.foreach { e => 
        log.debug(key(jobId, e))
        client.incr(key(jobId, e))
      }
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