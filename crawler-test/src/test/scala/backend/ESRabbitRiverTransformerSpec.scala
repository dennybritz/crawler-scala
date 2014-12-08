package org.blikk.test

import org.apache.commons.codec.binary.Base64
import org.blikk.crawler._
import org.blikk.crawler.Config
import org.blikk.crawler.processors.ESRabbitRiverTransformer
import org.scalatest.{FunSpec, Matchers}
import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._

class ESRabbitRiverTransformerSpec extends FunSpec with Matchers {

  case class SampleDocument(
    key: String,
    value: String
  )
  implicit val sampleDocumentJsonFormat = jsonFormat2(SampleDocument)

  describe("ESRabbitRiverTransformer") {

    describe("#transform") {

      it("should produce a valid bulk-request") {
        val sourceDoc = new SampleDocument("hello", "world").toJson

        val transformer = new ESRabbitRiverTransformer("indexName", "documentType")
        val resultStr = transformer.transform("someId", sourceDoc)
        val result = resultStr.split("\n")

        resultStr.endsWith("\n") shouldBe true
        
        result.size shouldEqual 2
        result(0).parseJson shouldEqual """
          {"index": {"_index": "indexName", "_type": "documentType", "_id": "someId"}}
        """.parseJson

        val source = result(1).parseJson.asJsObject
        source shouldEqual """
          {
            "key": "hello",
            "value": "world"
          }
        """.parseJson

      }
    }
  }
}