package org.blikk.crawler

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.JavaSerializer
import akka.actor._
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import scala.concurrent.duration._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}

object Frontier {
  def props(localRedis: RedisClientPool) = {
    Props(classOf[Frontier], localRedis)
  }
}

class Frontier(localRedis: RedisClientPool) 
  extends Actor with ActorLogging {

  import context.dispatcher
  import Parse.Implicits.parseByteArray

  val frontierKey = s"blikk:frontier"
  def requestObjectKey(uuid: String) = s"blikk:frontier:requestsObjects:${uuid}"
  val urlCacheKey = s"blikk:frontier:urlCache"

  val kryo = new Kryo()
  // TODO: Provide faster custom serialization
  kryo.register(classOf[spray.http.HttpRequest], new JavaSerializer())
  kryo.register(classOf[FetchRequest], new JavaSerializer())

  // Keeps track of frontier schedule
  var scheduleToken : Option[Cancellable] = None

  override def postStop() = { scheduleToken.foreach(_.cancel()) }

  /* Additional actor behavior */
  def receive = {
    case AddToFrontier(req, scheduledTime, ignoreDeduplication) =>
      log.info("adding to frontier: {} (scheduled: {})", req.req.uri.toString, scheduledTime)
      addToFrontier(req, scheduledTime, ignoreDeduplication)
    case StartFrontier(delay, target) =>
      log.info("starting frontier")
      startFrontier(delay, target)
    case StopFrontier =>
      log.info("stopping frontier")
      stopFrontier()
    case ClearFrontier =>
      log.info("clearing frontier")
      clearFrontier()
  }

  /* Schedules periodic checking of the frontier queue */
  def startFrontier(delay: FiniteDuration, target: ActorRef) : Unit = {
    stopFrontier()
    val cancelToken = context.system.scheduler.schedule(delay, delay) { 
      val requests = checkFrontier() 
      log.info("dequeued numRequests={}", requests.size)
      requests.foreach { r => target ! RouteFetchRequest(r) }
    }
    scheduleToken = Some(cancelToken)
  }

  /* Stops periodic checking of the frontier */
  def stopFrontier() {
    // TODO: This is somewhat ugly. We should refactor this actor into a FSM
    val _token = scheduleToken
    _token.foreach { token =>
      token.cancel()
      scheduleToken = None
    }
  }

  /* Removes all elements from the frontier. Use with care! */
  def clearFrontier() : Unit = {
    localRedis.withClient { client =>
      client.del(frontierKey)
      client.del(urlCacheKey)
      // Delete all request objects
      client.keys(requestObjectKey("")+"*").foreach { keys =>
        keys.flatten match {
          case Nil => // Do nothing
          case list => client.del(list.head, list.tail: _*) 
        }
      }
    }
  }

  /* Add a new request to the frontier */
  def addToFrontier(fetchReq: FetchRequest, scheduledTime: Option[Long],
    ignoreDeduplication: Boolean = false) : Unit = {
    localRedis.withClient { client =>
      /* Eliminate duplicate URLs */
      if(client.sadd(urlCacheKey,fetchReq.req.uri.toString) == Some(0l) && !ignoreDeduplication) {
        log.info("Ignoring url=\"{}\". Fetched previously.", fetchReq.req.uri.toString)
        return
      }
      val os = new ByteArrayOutputStream()
      val kryoOutput = new Output(os)
      val serializedObj = kryo.writeObject(kryoOutput, fetchReq)
      kryoOutput.close()
      val requestScore = scheduledTime.getOrElse(System.currentTimeMillis)
      client.set(requestObjectKey(fetchReq.req.uuid), os.toByteArray())
      client.zadd(frontierKey, requestScore, fetchReq.req.uuid)
    }
  }

  /* Get all outstanding requests from the frontier */
  def checkFrontier() : Seq[FetchRequest] = {
    localRedis.withClient { client =>
      client.pipeline { p =>
        val maxScore = System.currentTimeMillis
        // Get all UUIDs from the queue
        p.zrangebyscore[String](frontierKey, 0, true, maxScore, true, None)
        p.zremrangebyscore(frontierKey, 0, maxScore)
      }.get.apply(0).asInstanceOf[Option[List[String]]] match { 
        case Some(uuids) => getRequestObjects(uuids.map(requestObjectKey))
        case _ => Seq.empty
      }
    }
  }

  def getRequestObjects(keys: Seq[String]) : Seq[FetchRequest] = {
    if (keys.isEmpty) return List.empty
    localRedis.withClient { client =>
      client.mget[Array[Byte]](keys.head, keys.tail: _*).map { byteObjects =>
        byteObjects.flatten.map { buffer =>
          val is = new ByteArrayInputStream(buffer)
          kryo.readObject(new Input(is), classOf[FetchRequest]).asInstanceOf[FetchRequest]
        }
      }.getOrElse(Seq.empty)
    }
  }


}