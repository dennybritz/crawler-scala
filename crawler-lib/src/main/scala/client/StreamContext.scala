package org.blikk.crawler.client

import akka.stream.scaladsl._
import akka.actor._
import akka.stream._
import akka.stream.actor._
import com.rabbitmq.client.{Connection => RabbitConnection}

case class StreamContext[A](flow: Flow[A], api: ActorRef)
  (implicit _system: ActorSystem, _rabbitConn: RabbitConnection, _materializer: FlowMaterializer) {

  implicit val materializer = _materializer
  implicit val system = _system
  implicit val rabbitConnection = _rabbitConn

  def shutdown(){
    _system.shutdown()
  }

}