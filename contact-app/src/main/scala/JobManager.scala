package org.blikk.contactapp

import akka.actor._
import com.rabbitmq.client.{Channel => RabbitChannel}
import org.blikk.crawler._
import org.blikk.crawler.app._
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.Future
import JsonProtocols._
import spray.json._

object JobManager {
  def props = Props(classOf[JobManager])
}

class JobManager extends Actor with ActorLogging {

  implicit val _system = context.system
  import context.dispatcher

  val maxPages = 100
  val activeJobs = MutableMap[String, StreamContext[CrawlItem]]()

  def receive = {
    case msg @ StartJob(url, appId, timeLimit) =>
      //val appId = s"com.bikk.contactapp.${java.util.UUID.randomUUID()}"
      log.info("starting new job {} with appID={}", msg, appId)
      sender ! appId
      startJob(appId, url, timeLimit)
    case Terminated(someActor) =>
      activeJobs.find(_._2.publisher == someActor) foreach { case Tuple2(appId, streamContext) =>
        log.info("app={} has shut down", appId)
        // Publish a terminated event to RabbitMQ
        publishTerminatedEvent(appId)
        streamContext.shutdown()
        activeJobs.remove(appId)
      }
    case Shutdown =>
      log.info("shutting down")
      activeJobs.values.foreach(_.shutdown())
      context.system.shutdown()
  }

  def publishTerminatedEvent(appId: String){
    Resource.using(RabbitData.createChannel()) { channel =>
      log.info("Emitting termination event to RabbitMQ")
      val eventStr = Event("", "terminated", System.currentTimeMillis).toJson.compactPrint
      val routingKey = appId + "-out"
      channel.basicPublish(RabbitData.DefaultExchange.name, routingKey, null, eventStr.getBytes)
    }
  }

  def startJob(appId: String, startUrl: String, timeLimit: FiniteDuration) : Unit = {
    // Create a new app and stream context
    val app = new CrawlerApp(appId)
    val streamContext = app.start[CrawlItem]()
    // We watch the producer actor to shutdown the context on termination
    context.watch(streamContext.publisher)
    activeJobs.put(appId, streamContext)
    Future {
      ContactExtractionFlow.create(maxPages, timeLimit, startUrl)(streamContext)    
    }
  }
   
}