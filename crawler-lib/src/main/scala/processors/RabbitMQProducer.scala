package org.blikk.crawler.processors

import org.blikk.crawler._
import org.blikk.crawler.channels.RabbitMQChannelInput
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import scala.collection.JavaConversions._

class RabbitMQProducer(val name: String, connectionString: String, queue: String)
  extends ResponseProcessor {

  def process(res: WrappedHttpResponse, req: WrappedHttpRequest, jobConf: JobConfiguration, 
    context: Map[String, ProcessorOutput]) : Map[String, ProcessorOutput] = {
    Map(name -> RabbitMQChannelInput(connectionString, queue, getMessages(res)))
  }

  def getMessages(res: WrappedHttpResponse) : List[String] = {
    // Return the raw HTML string
    List(res.entity.asString)
  }

}