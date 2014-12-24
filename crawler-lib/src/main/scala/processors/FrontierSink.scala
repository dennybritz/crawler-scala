package org.blikk.crawler.processors

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.blikk.crawler.{RabbitData, Resource, FetchRequest, SerializationUtils}
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}

object FrontierSink {

  import RabbitData._

  /** 
    * Builds a sink that sends incoming requests to the crawler frontier
    */ 
  def build(appId: String)(implicit system: ActorSystem) : Sink[WrappedHttpRequest] = {
    val rabbitChannel = RabbitData.createChannel()
    RabbitMQSink.build[WrappedHttpRequest](
      rabbitChannel, 
      RabbitData.FrontierExchange) { req =>
      val fetchReq = FetchRequest(req, appId)
      val serializedItem = SerializationUtils.toProto(fetchReq).toByteArray
      (serializedItem, req.topPrivateDomain)
    }
  }
 
}