package org.blikk.crawler

import akka.actor.ActorRef
import scala.util.Try
import scala.concurrent.duration.FiniteDuration

case class InitializeFetcher(host: String)

case class FetchRequest(req: WrappedHttpRequest, jobId: String)
case class FetchResponse(res: WrappedHttpResponse, req: WrappedHttpRequest, jobId: String)
case class RouteFetchRequest(fetchReq: FetchRequest)

case class AddProcessor(proc: ResponseProcessor, host: String)

case class RunJob(job: JobConfiguration)
case class RegisterJob(job: JobConfiguration)
case class GetJob(jobId: String)

case class JobEvent(jobId : String, event: Any)
case class GetJobEventCount(jobId: String, event: Any)
case class GetJobEventCounts(jobId: String)
case class ClearJobEventCounts(jobId: String)
case class JobStats(jobId: String, eventCounts: Map[String, Int])
case class GetGlobalJobStats(jobId: String)
case class TerminateJob(jobId: String)

case class StartFrontier(delay: FiniteDuration, target: ActorRef)
case object StopFrontier
case object ClearFrontier
case class AddToFrontier(req: WrappedHttpRequest, jobId: String)

// Some Job Events