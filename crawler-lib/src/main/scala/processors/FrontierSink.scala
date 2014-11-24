package org.blikk.crawler.processors

import akka.stream.scaladsl.Sink
import org.blikk.crawler.{RabbitData, Resource, FetchRequest, SerializationUtils}
import org.blikk.crawler.app.StreamContext
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}

object FrontierSink {

  import RabbitData._

  /** 
    * Builds a sink that sends incoming requests to the crawler frontier
    */ 
  def build()(implicit ctx: StreamContext[_]) : Sink[WrappedHttpRequest] = {
    import ctx.system
    val appId = ctx.appId
    RabbitMQSink.build[WrappedHttpRequest](
      ctx.rabbitChannel, 
      RabbitData.FrontierExchange) { req =>
      val fetchReq = FetchRequest(req, appId)
      val serializedItem = SerializationUtils.toProto(fetchReq).toByteArray
      List((serializedItem, req.host))
    }
  }

}