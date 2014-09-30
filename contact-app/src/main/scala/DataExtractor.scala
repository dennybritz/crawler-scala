package org.blikk.contactapp

import scala.util.matching._

object DataExtractor {

  val emailRegex = """[A-z0-9._%+-]+@[A-z0-9.-]+\.[A-z]{2,4}\b""".r
  val phoneRegex = """(\+\d{1,2}\s)?\(?\d{3}\)?[\s.-]\d{3}[\s.-]\d{4}""".r

  def extract(input: String, src: String) : List[Extraction] = {
    // Extract all email addresses
    val emails = emailRegex.findAllIn(input).map { emailAddress =>
      Extraction(emailAddress, "email", src)
    }
    // Extract allphone numbers
    val phoneNumbers = phoneRegex.findAllIn(input).map { phone =>
      Extraction(phone, "phone", src)
    }
    (emails ++ phoneNumbers).toList
  }

}