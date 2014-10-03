package org.blikk.crawler

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
  entity: Array[Byte],
  headers: List[(String, String)],
  createdAt: Long) {

  def this(rawResponse: HttpResponse) = this(rawResponse.status, rawResponse.entity.data.toByteArray,
    rawResponse.headers.map(HttpHeader.unapply).map(_.get), System.currentTimeMillis)

  def stringEntity = new String(entity)

  lazy val contentType = headers.find { case (name, value) =>
    name.compareToIgnoreCase(HttpHeaders.`Content-Type`.name) == 0
  }.map(_._2)

}