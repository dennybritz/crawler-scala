package org.blikk.crawler.test

import org.scalatest._
import org.blikk.crawler._
import org.blikk.crawler.processors.LinkExtractor
import org.blikk.crawler.channels.FrontierChannelInput
import spray.http.Uri

class LinkExtractorSpec extends FunSpec {

  val jobConf = JobConfiguration.empty("testJob")

  describe("LinkExtractor") {

    it("should work with empty inputs") {
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty()
      val processor = new LinkExtractor("testExtractor", None)
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.map(_.uri.toString) === Seq.empty)
    }

    it("should work with absolute links") {
      val html = """<html><body><a href='http://google.com'>Hi.</a></body></html>"""
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.withContent(html)
      val processor = new LinkExtractor("testExtractor", None)
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.map(_.uri.toString) === Seq("http://google.com"))
    }

    it("should work with relative links") {
      val html = """<html><body><a href='/hello'>Hi.</a></body></html>"""
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.withContent(html)
      val processor = new LinkExtractor("testExtractor", None)
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.map(_.uri.toString).toSeq == Seq("http://localhost/hello"))
    }

    it("should ignore non-HTML responses") {
      val data = """{"key": "value"}"""
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.withContent(data)
      val processor = new LinkExtractor("testExtractor", None)
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.map(_.uri.toString).toSeq == Seq.empty)
    }

    it("should work with a filter function") {
      val html = """<html><body>
        <a href='http://localhost/hello'></a>
        <a href='http://localhost/world'></a>
      </body></html>"""
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.withContent(html)
      val processor = new LinkExtractor("testExtractor", Some({(src: Uri, req: Uri) =>  
        req.toString.contains("hello")}))
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result("testExtractor").isInstanceOf[FrontierChannelInput])
      assert(result("testExtractor").asInstanceOf[FrontierChannelInput]
        .newRequests.map(_.uri.toString).toSeq == Seq("http://localhost/hello"))
    }

    

  }

}