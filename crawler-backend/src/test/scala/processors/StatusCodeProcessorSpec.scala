package org.blikk.crawler.test

import org.scalatest._
import org.blikk.crawler._
import org.blikk.crawler.processors.StatusCodeProcessor
import org.blikk.crawler.channels.FrontierChannelInput
import spray.http.{Uri, HttpResponse, HttpHeaders, StatusCodes}

class StatusCodeProcessorSpec extends FunSpec {

  val jobConf = JobConfiguration.empty("testJob")

  describe("StatusCode Processor") {

    it("Should do nothing for 2xx responses") {
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty().copy(
        rawResponse = HttpResponse(StatusCodes.OK)
      )
      val processor = new StatusCodeProcessor("statusCodeProcessor")
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result === Map.empty)
    }

    it("Should extract the redirection URL for 3xx responses"){
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty().copy(
        rawResponse = HttpResponse(StatusCodes.PermanentRedirect,
          headers=List(HttpHeaders.Location(Uri("http://localhost"))))
      )
      val processor = new StatusCodeProcessor("statusCodeProcessor")
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result.contains("statusCodeProcessor"))
      val fci = result("statusCodeProcessor").asInstanceOf[FrontierChannelInput]
      assert(fci.newRequests.size == 1)
      assert(fci.newRequests.head.req.uri.toString === "http://localhost")
    }

    it("Should do nothing for 4xx responses") {
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty().copy(
        rawResponse = HttpResponse(StatusCodes.NotFound)
      )
      val processor = new StatusCodeProcessor("statusCodeProcessor")
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result === Map.empty)
    }

    it("Should do nothing for 5xx responses") {
      val req = WrappedHttpRequest.getUrl("http://localhost")
      val res = WrappedHttpResponse.empty().copy(
        rawResponse = HttpResponse(StatusCodes.ServiceUnavailable)
      )
      val processor = new StatusCodeProcessor("statusCodeProcessor")
      val result = processor.process(ResponseProcessorInput(res, req, jobConf))
      assert(result === Map.empty)
    }
  }



}