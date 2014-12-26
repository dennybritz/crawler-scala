package org.blikk.crawler.processors

import akka.actor.ActorSystem
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import com.blikk.serialization.HttpProtos
import org.blikk.crawler.{CrawlItem, RabbitData, SerializationUtils, RabbitPublisher}
import org.xerial.snappy.Snappy

object CrawledItemSource {

  def apply(appId: String)(implicit system: ActorSystem) : Source[CrawlItem] = {
    // Read new items from RabbitMQ
    val publisherActor = system.actorOf(RabbitPublisher.props(
      RabbitData.createChannel(), 
      RabbitData.queueForApp(appId), 
      RabbitData.DataExchange, 
      appId))
    val publisher = ActorPublisher[Array[Byte]](publisherActor)
    // Uncompresses and Deserializes the elements
    Source(publisher).map { element =>
      val uncompressedItem = Snappy.uncompress(element)
      SerializationUtils.fromProto(HttpProtos.CrawlItem.parseFrom(uncompressedItem))
    }
  }

}