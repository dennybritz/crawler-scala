package org.blikk.crawler

import com.redis.RedisClientPool
import akka.actor._
import scala.collection.mutable.{Map => MutableMap}
import com.redis.serialization.Parse

object JobStatsCollector {
  def props(localRedis: RedisClientPool, redisPrefix: String) = 
    Props(classOf[JobStatsCollector], localRedis, redisPrefix)

  object Keys {
    def numRequests(jobId: String, redisPrefix: String) = 
      s"${redisPrefix}stats:${jobId}:requests"
    def numRequests(jobId: String, host: String, redisPrefix: String) = 
      s"${redisPrefix}stats:${jobId}:${host}:requests"
    def numResponses(jobId: String, redisPrefix: String) = 
      s"${redisPrefix}stats:${jobId}:responses"
    def numResponses(jobId: String, host: String, redisPrefix: String) = 
      s"${redisPrefix}stats:${jobId}:${host}:responses"
  }
}

/* 
  Collects statistics about Jobs.
  Note: This runs locally on each node and only collects local job statistics.
  To obtain global job statistics across all nodes one must aggregate all local statistics. 
*/
class JobStatsCollector(localRedis: RedisClientPool, redisPrefix: String = "") 
  extends Actor with ActorLogging {

  import JobStatsCollector.Keys

  // def key(jobId: String, eventName: String) = s"stats:${jobId}:${eventName}"
  def eventKeys(jobId: String) = s"stats:${jobId}:events"

  override def preStart(){
    log.info("started")
    // Subscribe to the system event stream for job event
    context.system.eventStream.subscribe(self, classOf[JobEvent])
  }

  def receive = {
    case e @ JobEvent(jobId, event) =>
      increaseEventCounts(jobId, processJobEvent(e, jobId))
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
          client.del(jobId, eventName)
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
      import Parse.Implicits.parseInt
      client.mget[Int](eventNames.head, eventNames.tail: _*).map { values =>
        eventNames.map(stripPrefix).zip(values.flatten).toMap
      }.getOrElse(Map.empty)
    }
  }

  def increaseEventCounts(jobId: String, events: List[String]) ={
    localRedis.withClient { client =>
      client.sadd(eventKeys(jobId), events.head, events.tail: _*)
      events.foreach { e => 
        client.incr(e)
      }
    }
  }

  def processJobEvent(e: JobEvent, jobId: String) : List[String] = {
    e.event match {
      case req : WrappedHttpRequest =>
        List(Keys.numRequests(jobId, redisPrefix), Keys.numRequests(jobId, req.host, redisPrefix))
      case FetchResponse(res, req, jobId) =>
        List(Keys.numResponses(jobId, redisPrefix), Keys.numResponses(jobId, req.host, redisPrefix))
      case str : String => 
        List(jobId + ":" + str)
      case other => 
        log.warning("unhandled JobEvent type: {}", other)
        List.empty
    }
  }

  def stripPrefix(key: String) : String = {
    if (key.startsWith(redisPrefix))
      key.replaceFirst(redisPrefix, "")
    else
      key
  }



}