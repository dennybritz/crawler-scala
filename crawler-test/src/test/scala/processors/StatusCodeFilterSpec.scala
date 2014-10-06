package org.blikk.test

import org.blikk.crawler.{CrawlItem, WrappedHttpRequest}
import org.blikk.crawler.processors.StatusCodeFilter
import org.scalatest._
import spray.http.{HttpResponse, StatusCode}
import spray.http.StatusCodes._

class StatusCodeFilterSpec extends FunSpec with Matchers {

  val scf = new StatusCodeFilter()

  private def itemWithCode(code: StatusCode) = 
    CrawlItem(WrappedHttpRequest.empty, HttpResponse(code), "testJob")

  describe("StatusCodeFilter") {
    
    it("should allow 200 codes"){
     scf.filter(itemWithCode(OK)) shouldBe true
    }

    it("shuld allow 301") {
      scf.filter(itemWithCode(PermanentRedirect)) shouldBe true
    }

    it("should not allow 404") {
      scf.filter(itemWithCode(BadRequest)) shouldBe false
    }

    it("should not allow 503") {
      scf.filter(itemWithCode(ServiceUnavailable)) shouldBe false
    }

  }

}