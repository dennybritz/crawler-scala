package org.blikk.crawler.client

import akka.stream.scaladsl2.{FlowWithSource, FlowMaterializer}
import akka.actor._
import com.rabbitmq.client.{Connection => RabbitConnection}

case class StreamContext[A](flow: FlowWithSource[Array[Byte],A], api: ActorRef)
  (implicit _system: ActorSystem, _rabbitConn: RabbitConnection, _materializer: FlowMaterializer) {

  lazy val log = akka.event.Logging.getLogger(_system, this)
  implicit val materializer = _materializer
  implicit val system = _system
  implicit val rabbitConnection = _rabbitConn

  def shutdown(){
    log.info("shutting down")
    _system.shutdown()
    _system.awaitTermination()
  }

}