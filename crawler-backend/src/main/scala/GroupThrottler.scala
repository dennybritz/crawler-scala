package org.blikk.crawler

import akka.stream._
import scala.concurrent.duration._
import scala.collection.mutable.{Queue, Map => MMap}

class GroupThrottler[A](defaultInterval: FiniteDuration, 
  keyIntervals: Map[String, FiniteDuration] = Map.empty)(f: A => String) 
  extends TimerTransformer[A,A] with Logging {
  
  val buf = MMap.empty[String, Queue[A]].withDefault(x => Queue.empty[A])

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
    if (buf(elementKey).isEmpty){
      // Start a new schedule if we don't have one yet
      val scheduleInterval = keyIntervals.get(elementKey).getOrElse(defaultInterval)
      log.info(s"Starting new schedule for key='${elementKey}' intervalMs='${scheduleInterval.toMillis}'")
      schedulePeriodically(elementKey, scheduleInterval)
      buf(elementKey) = Queue.empty[A]
    }
    buf(elementKey) += element
    Nil
  }

}