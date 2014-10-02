package org.blikk.contactapp

import akka.stream.actor._
import akka.stream.OverflowStrategy
import akka.stream.actor.ActorSubscriberMessage._
import akka.stream.scaladsl2._
import akka.stream.scaladsl2.FlowGraphImplicits._
import com.rabbitmq.client.{Connection => RabbitMQConnection, ConnectionFactory => RabbitMQCF}
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicInteger
import JsonProtocols._
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.blikk.crawler.processors._
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import spray.json._

object ContactExtractionFlow {

  def create(maxPages: Int, maxTime: FiniteDuration, startUrl: String)
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
        Event(contactItem.toJson.compactPrint, "extracted", System.currentTimeMillis)
      }
    }.mapConcat(identity)

    // Sinks
    // ==================================================
    val dataSink = FoldSink[List[Event], Event](Nil) { _ :+ _}
    val rabbitSinkActor = ctx.system.actorOf(
      RabbitMQSink.props[Event](RabbitData.createChannel(), RabbitData.DefaultExchange) { item =>
      (item.toJson.compactPrint.getBytes, appId + "-out")
    }, s"sink-${ctx.appId}")
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
    val frontierMerge = Merge[WrappedHttpRequest]
    // Used to limit things we put into the frontier. Ugly, but needed to finish this quickly.
    val numFrontierRequests = new AtomicInteger(0);
    val frontierFilter = FlowFrom[WrappedHttpRequest].filter{ req =>
      numFrontierRequests.getAndIncrement() < maxPages
    }
    
    val mfg = FlowGraph { implicit b =>
      // Broadcast all items that were succesfully fetched
      src.takeWithin(maxTime).append(statusCodeFilter) ~> bcast
      // Exract links and request new URLs from the crawler
      bcast ~> reqExtractor.append(dupFilter) ~> frontierMerge
      // Extract contact information
      bcast ~> itemExtractor ~> eventMerge
      bcast ~> urlEventEmitter ~> eventMerge
      eventMerge ~> dataBroadcast
      // Various sinks
      bcast ~> terminationSink
      dataBroadcast ~> dataSink
      dataBroadcast ~> rabbitSink
      dataBroadcast ~> ForeachSink[Event] { event => log.info("appId={} event=\"{}\"", appId, event)}
      FlowFrom(seedUrls) ~> frontierMerge
      frontierMerge ~> frontierFilter ~> frontierSink
    }.run()

    // Handle the result
    dataSink.future(mfg).onComplete { 
      case Success(finalResult) =>
        ctx.shutdown()
        // Send a terminated event        
      case Failure(err) => 
        log.error(err.toString)
        ctx.shutdown()
    }

    mfg

  }

}