package org.blikk.crawler

import spray.json._
import DefaultJsonProtocol._
import org.apache.commons.codec.binary.Base64
 
/**
 * Transforms fetched pages into JSON strings that can be read by
 * https://github.com/elasticsearch/elasticsearch-river-rabbitmq/
 */
class ESRabbitRiverTransformer {

  case class EntityWithContentType(
    _content_type: Option[String],
    content: String
  )

  case class SourceRecord(
    timestamp: Long,
    request_uri: String,
    request_method: String,
    request_headers: List[(String, String)],
    request_provenance: List[String],
    request_entity: Option[EntityWithContentType],
    response_headers: List[(String, String)],
    response_status: Int,
    response_content_type: Option[String],
    response_entity: Option[EntityWithContentType]
  )

  case class MetadataRecord(
    _index: String,
    _type: String,
    _id: String
  )

  implicit val entityRecordFormat = jsonFormat2(EntityWithContentType)
  implicit val sourceRecordFormat = jsonFormat10(SourceRecord)
  implicit val metadataRecordFormat = jsonFormat3(MetadataRecord)

  def transform(fetchRes: FetchResponse) : String = {
    
    val actionRecord = Map(
      "index" -> MetadataRecord("crawler", "document", fetchRes.fetchReq.req.uri.toString)
    ).toJson.compactPrint

    // Only include the request and response entittoes if they are non-empty 
    val requestEntity = Base64.encodeBase64String(fetchRes.fetchReq.req.entity) match {
      case "" | null => None
      case x => Option(EntityWithContentType(None,x))
    }

    val responseEntity = Base64.encodeBase64String(fetchRes.res.entity) match {
      case "" | null => None
      case x => Option(EntityWithContentType(fetchRes.res.contentType, x))
    }

    val sourceRecord = SourceRecord(
      System.currentTimeMillis,
      fetchRes.fetchReq.req.uri.toString,
      fetchRes.fetchReq.req.method,
      fetchRes.fetchReq.req.headers,
      fetchRes.fetchReq.req.provenance,
      requestEntity,
      fetchRes.res.headers,
      fetchRes.res.status.intValue,
      fetchRes.res.contentType,
      responseEntity
    ).toJson.compactPrint

    return actionRecord + "\n" + sourceRecord + "\n"
  }


}