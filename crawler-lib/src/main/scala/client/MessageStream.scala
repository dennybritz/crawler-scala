package org.blikk.crawler.client

import akka.actor._
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._

object MessageStream {
  implicit def toFlow[A](m: MessageStream[A]) : Flow[A] = m.flow 
}

case class MessageStream[A](publisher: ActorRef, flow: Flow[A])(implicit system: ActorSystem) {
  def stop() : Unit = system.stop(publisher)
}