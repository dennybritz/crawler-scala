package org.blikk.crawler

import spray.http.HttpResponse
import spray.http.HttpEntity
import spray.http._
import java.util.UUID

object WrappedHttpResponse {
  
  implicit def fromSpray(res: HttpResponse) = WrappedHttpResponse(res)
  // implicit def toSpray(res: WrappedHttpResponse) = 
  //   HttpResponse(res.status, HttpEntity(res.stringEntity), res.headers)

  /* Returns an empty HTTP 200 response */
  def empty() : WrappedHttpResponse = WrappedHttpResponse(
    new HttpResponse(entity=HttpEntity.Empty)
  )

  /* Returns a response with the given string content and status code 200 */
  def withContent(data: String) = WrappedHttpResponse(
    new HttpResponse(entity=HttpEntity(data))
  )

  def apply(rawResponse: HttpResponse) : WrappedHttpResponse = 
    new WrappedHttpResponse(rawResponse)
}

/* A HttpResponse wrapped with additional information */ 
case class WrappedHttpResponse(
  status: StatusCode,
  stringEntity: String,
  headers: List[(String, String)],
  createdAt: Long) {
  def this(rawResponse: HttpResponse) = this(rawResponse.status, rawResponse.entity.asString,
    rawResponse.headers.map(HttpHeader.unapply).map(_.get), System.currentTimeMillis)
}