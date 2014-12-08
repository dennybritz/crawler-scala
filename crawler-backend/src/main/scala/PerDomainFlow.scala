package org.blikk.crawler

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import scala.concurrent.duration._

/* Throttles request based on the top-level private domain */
object PerDomainFlow {

  def apply(tpd: String, interval: FiniteDuration) = {
    val src = UndefinedSource[FetchRequest]
    val sink = UndefinedSink[FetchRequest]
    
    val tickSrc = Source(0.millis, interval, () => tpd)
    val timedZipper = Zip[String, FetchRequest]
    
    Flow[FetchRequest,FetchRequest] () { implicit b =>
      tickSrc ~> timedZipper.left
      src ~> timedZipper.right
      timedZipper.out ~> Flow[(String, FetchRequest)].map(_._2) ~> sink
      src -> sink
    }
  }

}