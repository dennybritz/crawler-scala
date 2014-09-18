package org.blikk.crawler

import akka.actor.ActorSystem

/* Extends a class with logging functionality */
trait Logging {
  lazy val log = org.slf4j.LoggerFactory.getLogger(this.getClass.getName)
}

/* Used for classes that are given an actor system but are not actors themselves */
trait ImplicitLogging {
  def system: ActorSystem
  lazy val log = akka.event.Logging.getLogger(system, this)
}