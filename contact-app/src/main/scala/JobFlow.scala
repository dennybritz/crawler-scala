package org.blikk.contactapp

import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.{Connection => RabbitMQConnection, ConnectionFactory => RabbitMQCF}
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.blikk.crawler.processors._
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import play.api.libs.json._

object ContactExtractionFlow {
  
  /* Initial RabbitMQ Setup */
  val config = ConfigFactory.load()
  val rabbitUrl = config.getString("org.blikk.contactapp.rabbitMQUrl")
  val rabbitExchangeName = config.getString("org.blikk.contactapp.rabbitMQExchange")
  val rabbitExchangeDef = RabbitExchangeDefinition(rabbitExchangeName, "direct", true)
  val rabbitFactory = new RabbitMQCF()
  rabbitFactory.setUri(rabbitUrl)
  val rabbitConn = rabbitFactory.newConnection()

  def create(appId: String, maxPages: Int, maxTime: FiniteDuration, startUrl: String)
  (implicit ctx: StreamContext[CrawlItem]) : MaterializedFlowGraph = {
    
    import ctx._
    import ctx.system.dispatcher

    val src = ctx.flow
    val seedUrls = List(WrappedHttpRequest.getUrl(startUrl))
    val dupFilter = DuplicateFilter.buildUrlDuplicateFilter(seedUrls)
    val frontierSink = FrontierSink.build()
    val reqExtractor = RequestExtractor.build()
    val statusCodeFilter = StatusCodeFilter.build()
    val itemExtractor = FlowFrom[CrawlItem].map { item =>
      DataExtractor.extract(item.res.stringEntity, item.req.uri.toString).map { contactItem =>
        Event(contactItem.toString, "extracted", System.currentTimeMillis)
      }
    }.mapConcat(identity)

    // Sinks
    // ==================================================
    val dataSink = FoldSink[List[Event], Event](Nil) { _ :+ _}
    val rabbitSinkActor = ctx.system.actorOf(
      RabbitMQSink.props[Event](rabbitConn, rabbitExchangeDef) { item =>
      // TODO: JSON
      (item.toString.getBytes, appId)
    })
    val rabbitSink = SubscriberSink(ActorSubscriber[Event](rabbitSinkActor))
    val terminationSink = TerminationSink.build {_.numFetched >= maxPages}

    // Broadcasts
    // ==================================================
    val bcast = Broadcast[CrawlItem]
    val dataBroadcast = Broadcast[Event]

    // Events and merging
    // ==================================================
    val urlEventEmitter = FlowFrom[CrawlItem].map { ci => 
      Event(ci.req.uri.toString, "url_processed", System.currentTimeMillis) }
    val eventMerge = Merge[Event]
    

    val mfg = FlowGraph { implicit b =>
      // Broadcast all items that were succesfully fetched
      src.takeWithin(maxTime).append(statusCodeFilter) ~> bcast
      // Exract links and request new URLs from the crawler
      bcast ~> reqExtractor.append(dupFilter).withSink(frontierSink)
      // Extract contact information
      bcast ~> itemExtractor ~> eventMerge
      bcast ~> urlEventEmitter ~> eventMerge
      eventMerge ~> dataBroadcast
      // Various sinks
      bcast ~> terminationSink
      dataBroadcast ~> dataSink
      dataBroadcast ~> rabbitSink
      dataBroadcast ~> ForeachSink[Event] { event => log.info("{}", event)}
    }.run()

    // Handle the result
    dataSink.future(mfg).onComplete { 
      case Success(finalResult) =>
        log.info(finalResult.toString)
        ctx.shutdown()
      case Failure(err) => 
        log.error(err.toString)
        ctx.shutdown()
    }

    mfg

  }

}