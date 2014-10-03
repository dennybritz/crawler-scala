package org.blikk.crawler

import com.typesafe.config._
import scala.concurrent.duration._

/* Convenience accessors for global configuration options */
object Config {

  lazy val config = ConfigFactory.load()

  lazy val requestTimeout = FiniteDuration(config.getMilliseconds("blikk.crawler.requestTimeOut"), "ms")
  lazy val perDomainDelay = FiniteDuration(config.getMilliseconds("blikk.crawler.perDomainDelay"), "ms")
  lazy val perDomainBuffer = config.getInt("blikk.crawler.perDomainBuffer")


}