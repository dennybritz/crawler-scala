package org.blikk.crawler

import com.typesafe.config._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

/* Convenience accessors for global configuration options */
object Config {

  lazy val config = ConfigFactory.load()

  lazy val requestTimeout = FiniteDuration(
    config.getDuration("blikk.crawler.requestTimeOut", TimeUnit.MILLISECONDS), "ms")
  lazy val perDomainDelay = FiniteDuration(
    config.getDuration("blikk.crawler.perDomainDelay", TimeUnit.MILLISECONDS), "ms")
  lazy val perDomainBuffer = config.getInt("blikk.crawler.perDomainBuffer")


}