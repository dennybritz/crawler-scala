package org.blikk.test

import org.blikk.crawler.WrappedHttpRequest
import org.blikk.crawler.processors.UrlDuplicateFilter
import org.scalatest._

class UrlDuplicateFilterSpec extends FunSpec with Matchers {

  describe("UrlDuplicatefilter") {
    it("should work"){
      val requestes = List("1","2","3","1","2","1","4","4","5").map { x => 
        WrappedHttpRequest.getUrl(s"http://${x}.com")
      }
      val filter = new UrlDuplicateFilter()
      val expectedResult = List("1","2","3","4","5").map(s => s"http://${s}.com")
      requestes.filter(filter.runThrough).map(_.uri.toString) should === (expectedResult)
    }
  }

}