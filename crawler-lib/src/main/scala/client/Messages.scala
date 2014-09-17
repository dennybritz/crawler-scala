package org.blikk.crawler

/* API Messages:  */
case class ApiRequest(payload: Any)
case class ApiResponse(payload: Any)
case class ApiError(reason: String)
object ApiResponse {
  val OK = ApiResponse("OK")
}

/* The API client requests connection information for RabbitMQ when it first starts */
case object ConnectionInfoRequest
case class ConnectionInfo(rabbitMQUri: String)

/* General Messages */
case class FetchRequest(req: WrappedHttpRequest, jobId: String)

/* Wrapper class for messages that are dequeued from RabbitMQ */
case class RabbitMessage(
  routingKey: String, 
  contentType: String, 
  deliveryTag: Long,
  payload: Array[Byte])