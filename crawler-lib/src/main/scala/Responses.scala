package org.blikk.crawler

trait Response extends Serializable

import spray.http.HttpResponse

// We use spray.io for HTTP responses

object WrappedHttpResponse {
  implicit def sprayConversion(res: HttpResponse) : WrappedHttpResponse = 
    new WrappedHttpResponse(res)
  implicit def sprayConversion(res: WrappedHttpResponse) : HttpResponse = 
    res.rawResponse
  def empty() : WrappedHttpResponse = new WrappedHttpResponse(
    new HttpResponse(entity=spray.http.HttpEntity.Empty)
  )
  def withContent(data: String) : WrappedHttpResponse = new WrappedHttpResponse(
    new HttpResponse(entity=spray.http.HttpEntity(data))
  )
}

case class WrappedHttpResponse(rawResponse: HttpResponse, timestamp: Long) extends Response {
  def this(rawResponse: HttpResponse) = this(rawResponse, System.currentTimeMillis)
}