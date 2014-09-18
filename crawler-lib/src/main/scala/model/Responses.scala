package org.blikk.crawler

import spray.http.HttpResponse
import spray.http.HttpEntity
import java.util.UUID

object WrappedHttpResponse {
  
  implicit def fromSpray(res: HttpResponse) = WrappedHttpResponse(res)
  implicit def toSpray(res: WrappedHttpResponse) = res.rawResponse

  /* Returns an empty HTTP 200 response */
  def empty() : WrappedHttpResponse = WrappedHttpResponse(
    new HttpResponse(entity=HttpEntity.Empty)
  )

  /* Returns a response with the given string content and status code 200 */
  def withContent(data: String) = WrappedHttpResponse(
    new HttpResponse(entity=HttpEntity(data))
  )

  def apply(rawResponse: HttpResponse) : WrappedHttpResponse = 
    WrappedHttpResponse(rawResponse, System.currentTimeMillis)
}

/* A HttpResponse wrapped with additional information */ 
case class WrappedHttpResponse(rawResponse: HttpResponse, createdAt: Long) {
  def this(rawResponse: HttpResponse) = this(rawResponse, System.currentTimeMillis)
}