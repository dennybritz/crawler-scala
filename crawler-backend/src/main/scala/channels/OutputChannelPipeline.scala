package org.blikk.crawler.channels

import akka.actor.{ActorSystem, ActorRef}
import org.blikk.crawler._
import org.blikk.crawler.processors._
import com.typesafe.config._
import scala.collection.JavaConverters._

class OutputputChannelPipeline(serviceActor: ActorRef)
  (implicit system: ActorSystem) extends Logging {

  val rabbitMQChannel = new RabbitMQChannel() 
  val frontierOutputChannel = new FrontierOutputChannel(serviceActor) 
  val jobOutputChannel = new JobOutputChannel()

  def process(in: ResponseProcessorInput) : Unit = {
    in.context.values.foreach {
      case x : RabbitMQChannelInput => rabbitMQChannel.pipe(x, in.jobConf, in.jobStats)
      case x : FrontierChannelInput => frontierOutputChannel.pipe(x, in.jobConf, in.jobStats)
      case x : JobChannelInput => jobOutputChannel.pipe(x, in.jobConf, in.jobStats)
      case other => log.warn(s"Unknown channel input type: ${other.getClass.getName}")
    }
  }

}