package org.blikk.crawler.processors

import spray.json._
import DefaultJsonProtocol._
import org.apache.commons.codec.binary.Base64
 
object ESRabbitRiverTransformer {

  case class MetadataRecord(
    _index: String,
    _type: String,
    _id: String
  )

  implicit val metadataRecordFormat = jsonFormat3(MetadataRecord)
}

/**
 * Transforms documents into bulk API records that can be processed by
 * https://github.com/elasticsearch/elasticsearch-river-rabbitmq/
 */
class ESRabbitRiverTransformer(index: String, recordType: String) {

  import ESRabbitRiverTransformer._

  def transform(id: String, sourceRecord: JsValue) : String = {
    val actionRecord = Map(
      "index" -> MetadataRecord(index, recordType, id)
    ).toJson.compactPrint
    actionRecord + "\n" + sourceRecord.compactPrint + "\n"
  }
}