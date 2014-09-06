package org.blikk.crawler

import scala.util.Try

case class InitializeFetcher(host: String)

case class FetchRequest(req: WrappedHttpRequest, jobId: String)
case class FetchResponse(res: WrappedHttpResponse, req: WrappedHttpRequest, jobId: String)

case class AddProcessor(proc: ResponseProcessor, host: String)

case class RunJob(job: JobConfiguration)
case class RegisterJob(job: JobConfiguration)
case class GetJob(jobId: String)