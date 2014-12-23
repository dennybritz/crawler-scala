package org.blikk.crawler

import akka.actor.ActorRef
import scala.concurrent.duration._

/* Commands for the frontier. These are not available on the client */
trait FrontierCommand
case class StartFrontier(delay: FiniteDuration, target: ActorRef) extends FrontierCommand
case object StopFrontier extends FrontierCommand
case object ClearFrontier extends FrontierCommand