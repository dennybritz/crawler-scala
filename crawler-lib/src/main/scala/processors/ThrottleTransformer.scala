package org.blikk.crawler.processors

import org.blikk.crawler.Logging
import akka.stream._
import scala.concurrent.duration._

class ThrottleTransformer[A](interval: FiniteDuration) extends TimerTransformer[A,A] with Logging {
  
  var buf = Vector.empty[A]

  schedulePeriodically("tick", interval)

  override def onTimer(timerKey: Any): scala.collection.immutable.Seq[A] = {
    val (use, keep) = buf.splitAt(1)
    buf = keep
    use
  }

  override def onNext(element: A): scala.collection.immutable.Seq[A] = {
    buf :+= element
    Nil
  }

  override def isComplete: Boolean = !isTimerActive("tick")


}