package org.blikk.contactapp

import akka.actor._
import com.rabbitmq.client.{Connection => RabbitMQConnection, ConnectionFactory => RabbitMQCF}
import org.blikk.crawler._
import org.blikk.crawler.app._
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap}

object JobManager {

  def props(apiEndpoint: String) = Props(classOf[JobManager], apiEndpoint)

}

class JobManager(apiEndpoint: String) extends Actor with ActorLogging {

  implicit val _system = context.system
  import context.dispatcher

  val maxPages = 500
  val activeJobs = MutableMap[String, StreamContext[CrawlItem]]()

  def receive = {
    case msg @ StartJob(url, callbackUrl, timeLimit) =>
      val appId = java.util.UUID.randomUUID().toString
      log.info("starting new job {} with appID={}", msg, appId)
      sender ! appId
      startJob(appId, url, callbackUrl, timeLimit)
    case Terminated(someActor) =>
      activeJobs.find(_._2.publisher == someActor) foreach { case Tuple2(appId, streamContext) =>
        log.info("app={} has shut down", appId)
        streamContext.shutdown()
        activeJobs.remove(appId)
      }
    case Shutdown =>
      log.info("shutting down")
      activeJobs.values.foreach(_.shutdown())
      context.system.shutdown()
  }

  def startJob(appId: String, startUrl: String, callbackUrl: String, timeLimit: FiniteDuration) : Unit = {
    // Create a new app and stream context
    val app = new CrawlerApp(apiEndpoint, appId)
    val streamContext = app.start[CrawlItem]()
    activeJobs.put(appId, streamContext) 
    ContactExtractionFlow.create(maxPages, timeLimit, startUrl)(streamContext)
    // We watch the producer actor to shutdown the context on termination
    context.watch(streamContext.publisher)
  }
   
}