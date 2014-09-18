package org.blikk.crawler

import akka.actor.ActorRef
import scala.concurrent.duration._

/* Routes a Fetch Request to the apporpriate cluster nodes */
case class RouteFetchRequest(req: FetchRequest)

/* Commands for the frontier. These are not available on the client */
trait FrontierCommand
case class StartFrontier(delay: FiniteDuration, target: ActorRef) extends FrontierCommand
case object StopFrontier extends FrontierCommand
case object ClearFrontier extends FrontierCommand
case class AddToFrontier(
  req: FetchRequest,
  scheduledTime : Option[Long] = None,
  ignoreDeduplication : Boolean = false) extends FrontierCommand

/* Convenience wrapper for a fetch response */
case class FetchResponse(fetchReq: FetchRequest, res: WrappedHttpResponse)