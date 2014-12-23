package org.blikk.crawler.model

import org.apache.commons.codec.binary.Base64
import org.blikk.crawler._
import spray.json._
import DefaultJsonProtocol._

object ESJsonTransformations {

  implicit object FetchResponseJsonFormat extends JsonFormat[FetchResponse] {

    def write(fetchRes: FetchResponse) = {
      import FetchResponseESModels._
      implicit val EntityRecordFormat = jsonFormat2(EntityWithContentType)

      // Only include the request and response entittoes if they are non-empty 
      val requestEntity = Base64.encodeBase64String(fetchRes.fetchReq.req.entity) match {
        case "" | null => None
        case x => Option(EntityWithContentType(None,x))
      }

      val responseEntity = Base64.encodeBase64String(fetchRes.res.entity) match {
        case "" | null => None
        case x => Option(EntityWithContentType(fetchRes.res.contentType, x))
      }

      JsObject(
        Map(
          "timestamp" -> System.currentTimeMillis.toJson,
          "request_uri" -> fetchRes.fetchReq.req.uri.toString.toJson,
          "request_method" -> fetchRes.fetchReq.req.method.toJson,
          "request_headers" -> fetchRes.fetchReq.req.headers.toJson,
          "request_provenance" -> fetchRes.fetchReq.req.provenance.toJson,
          "request_entity" -> requestEntity.toJson,
          "response_headers" -> fetchRes.res.headers.toJson,
          "response_status" -> fetchRes.res.status.intValue.toJson,
          "response_content_type" -> fetchRes.res.contentType.toJson,
          "response_entity" -> responseEntity.toJson
        ).filterNot { case (key, value) => value == JsNull }
      )
    }

    def read(value: JsValue) = {
      deserializationError("Cannot deserialize FetchResponse JSON. It's for ElasticSearch writing only!")
    }
  }

}

object FetchResponseESModels {

  case class EntityWithContentType(
    _content_type: Option[String],
    _content: String
  )

}