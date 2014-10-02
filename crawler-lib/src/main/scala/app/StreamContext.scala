package org.blikk.crawler.app

import akka.actor._
import akka.stream.scaladsl2.{FlowWithSource, FlowMaterializer}
import com.rabbitmq.client.{Connection => RabbitConnection}
import org.blikk.crawler.ImplicitLogging

/** 
  * The context for a running streaming application.
  * Within a running application, you can interact with the API client `api`
  * The flow of the streaming context can only be consumed once.
  */
case class StreamContext[A](appId: String, flow: FlowWithSource[Array[Byte],A], publisher: ActorRef)
  (implicit _system: ActorSystem, _rabbitConn: RabbitConnection, _materializer: FlowMaterializer) 
  extends ImplicitLogging {

  implicit val materializer = _materializer
  implicit val system = _system
  implicit val rabbitConnection = _rabbitConn

  def shutdown(){
    system.synchronized {
      log.info("shutting down")
      _system.stop(publisher)
    }
  }

}