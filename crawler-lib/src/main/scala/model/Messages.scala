package org.blikk.crawler

/* API Messages: */
/* ================================================== */

case class ApiRequest(payload: Any)
case class ApiResponse(payload: Any)
case class ApiError(reason: String)
object ApiResponse {
  val OK = ApiResponse("OK")
}

/* General Messages */
/* ================================================== */

/* Instructs the crawler to fetch the requested URL */
case class FetchRequest(req: WrappedHttpRequest, appId: String)

/* Wrapper class for messages that are dequeued from RabbitMQ */
case class RabbitMessage(
  routingKey: String, 
  contentType: String, 
  deliveryTag: Long,
  payload: Array[Byte])