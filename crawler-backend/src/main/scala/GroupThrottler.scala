package org.blikk.crawler

import akka.stream._
import scala.concurrent.duration._
import scala.collection.mutable.{Queue, Map}

class GroupThrottler[A](interval: FiniteDuration)(f: A => String) 
  extends TimerTransformer[A,A] with Logging {
  
  val buf = Map.empty[String, Queue[A]].withDefault(x => Queue.empty[A])

  override def onTimer(timerKey: Any): scala.collection.immutable.Seq[A] = {
    val elementKey = timerKey.toString
    if(buf(elementKey).isEmpty) {
      // Buffer for the key is empty, cancel the timer
      log.info("Canceling schedule for key=\"{}\"", elementKey)
      cancelTimer(elementKey)
      Nil
    }
    else {
      val result = buf(elementKey).dequeue
      List(result)
    }
  }

  override def onNext(element: A): scala.collection.immutable.Seq[A] = {
    val elementKey = f(element)
    // Start scheduling messages if we haven't yet
    if (buf(elementKey).isEmpty){
      log.info("Starting new schedule for key=\"{}\"", elementKey)
      schedulePeriodically(elementKey, interval)
      buf(elementKey) = Queue.empty[A]
    }
    buf(elementKey) += element
    Nil
  }

}