package org.blikk.contactapp.test

import org.scalatest._
import org.blikk.contactapp._

class DataExtractorSpec extends FunSpec with Matchers {

  describe("DataExtractor") {

    it("should extract email addresses") {
      val input = """
      I am text with denny@blikk.co some email addresses
      in it: someone@gmail.com
      """
      val extractions = DataExtractor.extract(input, "testSource")
      extractions.shouldEqual (List(
        Extraction("denny@blikk.co", "email", "testSource"), 
        Extraction("someone@gmail.com", "email", "testSource")))
    }

    it("should extract phone numbers") {
      val input = """
      Somephone numbers:
      - 310-295-7219
      - (310) 295-7219
      - 310.295.7219
      """
      val extractions = DataExtractor.extract(input, "testSource")
      extractions.shouldEqual (List(
        Extraction("310-295-7219", "phone", "testSource"), 
        Extraction("(310) 295-7219", "phone", "testSource"),  
        Extraction("310.295.7219", "phone", "testSource")))
    }

  }

}