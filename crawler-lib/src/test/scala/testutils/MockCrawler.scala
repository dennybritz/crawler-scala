package org.blikk.test

import akka.actor._
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._
import com.blikk.serialization._
import org.blikk.crawler._
import org.blikk.crawler.processors._
import org.xerial.snappy.Snappy

object MockCrawler {

  def requestToCrawlItem : Flow[FetchRequest, CrawlItem] = Flow[FetchRequest].map { fetchReq =>
    val mockResponse =  WrappedHttpResponse.withContent("OK")
    CrawlItem(fetchReq.req, mockResponse, fetchReq.appId)
  }

  def apply()(implicit system: ActorSystem) : RunnableFlow = {
    MockCrawler(Flow[CrawlItem])
  }

  def apply(op: Flow[CrawlItem, CrawlItem])(implicit system: ActorSystem) : RunnableFlow = {

    // Reads requests
    val requestPublisherActor = system.actorOf(RabbitPublisher.props(RabbitData.createChannel(),
      RabbitData.FrontierQueue, RabbitData.FrontierExchange, "#"), "mockCrawler-requestPublisher")
    val requestSource: Source[FetchRequest] = Source(ActorPublisher[Array[Byte]](requestPublisherActor)).map { element =>
      SerializationUtils.fromProto(HttpProtos.FetchRequest.parseFrom(element))
    }

    // Publishes crawled items
    val dataSink = RabbitMQSink.build[CrawlItem](RabbitData.createChannel(), RabbitData.DataExchange) { item =>
      val serializedItem = SerializationUtils.toProto(item).toByteArray
      val compressedItem =  Snappy.compress(serializedItem)
      (compressedItem, item.appId)
    }
    
    requestSource.via(requestToCrawlItem).via(op).to(dataSink)
  }

}