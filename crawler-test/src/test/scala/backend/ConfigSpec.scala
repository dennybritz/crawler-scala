package org.blikk.test

import org.scalatest.{FunSpec, Matchers}
import org.blikk.crawler.Config
import scala.concurrent.duration._

class ConfigSpec extends FunSpec with Matchers {

  describe("Crawler Config") {

    describe("#customDomainDelays") {

      it ("should work") {
        Config.customDomainDelays should contain ("somedomain.com" -> FiniteDuration(1337, "ms"))
      }

    }

  }

}