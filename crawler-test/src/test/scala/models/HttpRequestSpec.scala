package org.blikk.test

import org.blikk.crawler._
import org.scalatest._
import com.blikk.serialization.HttpProtos

class WrappedHttpRequestSpec extends FunSpec with Matchers {

  describe("Wrapped Http Request") {

    it("should return the correct base URI") {
      WrappedHttpRequest.getUrl("http://www.google.com/some/base/uri-comes-here?aa")
        .baseUri shouldEqual ("http://www.google.com/some/base/uri-comes-here")
      
      WrappedHttpRequest.getUrl("http://www.google.com/some/base/uri-comes-here")
        .baseUri shouldEqual ("http://www.google.com/some/base/uri-comes-here")
      
      WrappedHttpRequest.getUrl("http://www.google.com/some/base/")
        .baseUri shouldEqual ("http://www.google.com/some/base/")
    }

    it("should return the correct base top private domain") {
      WrappedHttpRequest.getUrl("http://www.google.com/some/base/uri-comes-here?aa")
        .topPrivateDomain.get shouldEqual ("google.com")
      WrappedHttpRequest.getUrl("http://amazon.co.jp")
        .topPrivateDomain.get shouldEqual ("amazon.co.jp")
      WrappedHttpRequest.getUrl("http://blog.amazon.co.jp")
        .topPrivateDomain.get shouldEqual ("amazon.co.jp")
    }

  }

  describe("Serialization") {
    
    it("Should work") {
      val req =  WrappedHttpRequest.getUrl("http://www.google.com/some/base/uri-comes-here?aa")
        .copy(headers = List("h1" -> "v1"))

      val serReq = SerializationUtils.toProto(req).toByteArray()
      val desReq = SerializationUtils.fromProto(HttpProtos.HttpRequest.parseFrom(serReq))
      desReq.method shouldEqual req.method
      desReq.uri shouldEqual req.uri
      desReq.headers shouldEqual req.headers
    }

  }

}