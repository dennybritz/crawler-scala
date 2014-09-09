package org.blikk.crawler

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.JavaSerializer
import akka.actor._
import com.redis.RedisClient
import com.redis.serialization.{Format, Parse}
import scala.concurrent.duration._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}

object Frontier {
  def props(jobId: String, localRedis: RedisClient) = Props(classOf[Frontier], jobId, localRedis)
}

class Frontier(jobId: String, localRedis: RedisClient) 
  extends Actor with ActorLogging {

  import context.dispatcher
  import Parse.Implicits.parseByteArray

  val frontierKey = s"local:${jobId}:frontier"
  def requestObjectKey(uuid: String) = s"local:${jobId}:requestsObjects:${uuid}"
  val urlCacheKey = s"local:${jobId}:urlCache"

  val kryo = new Kryo()
  // TODO: Provide faster custom serialization
  kryo.register(classOf[spray.http.HttpRequest], new JavaSerializer())
  kryo.register(classOf[WrappedHttpRequest], new JavaSerializer())

  // Keeps track of frontier schedule
  var jobToken : Option[Cancellable] = None

  override def postStop() = { jobToken.foreach(_.cancel()) }

  /* Additional actor behavior */
  def receive = {
    case AddToFrontier(req, _) =>
      log.debug("adding to frontier for job=\"{}\": {}", jobId, req.uuid)
      addToFrontier(req)
    case StartFrontier(delay, target) =>
      log.info("starting frontier for job=\"{}\"", jobId)
      startFrontier(delay, target)
    case StopFrontier =>
      log.info("stopping frontier for job=\"{}\"", jobId)
      stopFrontier()
    case ClearFrontier =>
      log.info("clearning frontier for job=\"{}\"", jobId)
      clearFrontier()
  }

  /* Schedules periodic checking of the frontier queue */
  def startFrontier(delay: FiniteDuration, target: ActorRef) : Unit = {
    val cancelToken = context.system.scheduler.schedule(delay, delay) { 
      val requests = checkFrontier() 
      log.debug("dequeued numRequests={}", requests.size)
      requests.foreach { r => target ! FetchRequest(r, jobId) }
    }
    jobToken = Some(cancelToken)
  }

  /* Stops periodic checking of the frontier */
  def stopFrontier() {
    jobToken.foreach(_.cancel())
  }

  /* Removes all elements from the frontier for the given job */
  def clearFrontier() : Unit = {
    localRedis.del(frontierKey)
    localRedis.del(urlCacheKey)
  }

  /* Add a new request to the frontier */
  def addToFrontier(req: WrappedHttpRequest) : Unit = {
    /* Eliminate duplicate URLs */
    if(localRedis.sadd(urlCacheKey,req.uri.toString) == Some(0l)) {
      log.debug("Ignoring url=\"{}\". Fetched previously.", req.uri.toString)
      return
    }
    val os = new ByteArrayOutputStream()
    val kryoOutput = new Output(os)
    val serializedObj = kryo.writeObject(kryoOutput, req)
    kryoOutput.close()
    val requestScore = req.scheduledTime.getOrElse(System.currentTimeMillis)
    localRedis.set(requestObjectKey(req.uuid), os.toByteArray())
    localRedis.zadd(frontierKey, requestScore, req.uuid)
  }

  /* Get all outstanding requests from the frontier */
  def checkFrontier() : Seq[WrappedHttpRequest] = {
    val maxScore = System.currentTimeMillis
    // Get all UUIDs from the queue
    val result = localRedis.zrangebyscore[String](frontierKey, 0, true, maxScore, true, None) match { 
      case Some(Nil) => Seq.empty
      case Some(uuids) => 
        getRequestObjects(uuids.map(requestObjectKey))
      case None => Seq.empty
    }
    // Remove the elements we just pulled from redis
    // TODO: Perhaps we should remove this after the request has been processed.
    localRedis.zremrangebyscore(frontierKey, 0, maxScore)
    return result
  }

  def getRequestObjects(keys: Seq[String]) : Seq[WrappedHttpRequest] = {
    localRedis.mget[Array[Byte]](keys.head, keys.tail: _*).map { byteObjects =>
      byteObjects.flatten.map { buffer =>
        val is = new ByteArrayInputStream(buffer)
        kryo.readObject(new Input(is), classOf[WrappedHttpRequest]).asInstanceOf[WrappedHttpRequest]
      }
    }.getOrElse(Seq.empty)
  }


}