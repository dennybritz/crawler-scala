package org.blikk.test

import org.blikk.crawler.WrappedHttpRequest
import org.blikk.crawler.processors.StringDuplicateFilter
import org.scalatest._

class DuplicateFilterSpec extends FunSpec with Matchers {

  describe("Duplicate Filter") {
    it("should work"){
      val items = List("1","2","3","1","2","1","4","4","5")
      val df = new StringDuplicateFilter(10000, 0.0001)
      val expectedResult = List("1","2","3","4","5")
      items.filter(df.filter) should === (expectedResult)
    }
  }

}