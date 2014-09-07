package org.blikk.crawler.channels

import akka.actor.ActorSystem
import org.blikk.crawler.Logging
import org.blikk.crawler.processors._
import org.blikk.crawler.ProcessorOutput
import com.typesafe.config._
import scala.collection.JavaConverters._

class OutputputChannelPipeline (implicit system: ActorSystem) extends Logging {

  val rabbitMQChannel = new RabbitMQChannel() 
  val frontierOutputChannel = new FrontierOutputChannel() 

  def process(data: Map[String, ProcessorOutput]) : Unit = {
    data.values.foreach {
      case x : RabbitMQChannelInput => rabbitMQChannel.pipe(x)
      case x : FrontierChannelInput => frontierOutputChannel.pipe(x)
      case other => log.warn(s"Unknown channel input type: ${other.getClass.getName}")
    }
  }

}