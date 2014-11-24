package org.blikk.test

import org.apache.commons.codec.binary.Base64
import org.blikk.crawler._
import org.blikk.crawler.Config
import org.scalatest.{FunSpec, Matchers}
import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._

class ESRabbitRiverTransformerSpec extends FunSpec with Matchers {

  describe("ESRabbitRiverTransformer") {

    describe("#transform") {

      it("should work") {
        val httpReq = WrappedHttpRequest("GET", new java.net.URI("http://blikk.co/test"), 
          List.empty, Array.empty[Byte])
        val fetchReq = FetchRequest(httpReq, "someApp")
        val httpRes = WrappedHttpResponse(200, "OK!".getBytes(),
          List(Tuple2("content-type", "text/html")))
        val fetchRes = FetchResponse(fetchReq, httpRes)

        val transformer = new ESRabbitRiverTransformer()
        val result = transformer.transform(fetchRes)

        result.size shouldEqual 2
        result(0).parseJson shouldEqual """
          {"index": {"_index": "crawler", "_type": "FetchedPage", "_id": "http://blikk.co/test"}}
        """.parseJson

        val source = result(1).parseJson.asJsObject
        
        source.fields("timestamp").convertTo[Long] shouldBe System.currentTimeMillis +- 1000
        
        JsObject(source.fields filterKeys Set("request_uri", "request_method", "request_headers", 
          "request_provenance", "request_entity")) shouldEqual """
          {
            "request_uri": "http://blikk.co/test",
            "request_method": "GET",
            "request_headers": [],
            "request_provenance": [],
            "request_entity": {
              "content": ""
            }
          }
        """.parseJson

        JsObject(source.fields filterKeys Set("response_status", "response_headers", 
          "response_content_type", "response_entity")) shouldEqual s"""
          {
            "response_status": 200,
            "response_headers": [["content-type", "text/html"]],
            "response_content_type": "text/html",
            "response_entity": {
              "_content_type": "text/html",
              "content": "${Base64.encodeBase64String("OK!".getBytes)}"
            }
          }
        """.parseJson
      }

    }

  }

}