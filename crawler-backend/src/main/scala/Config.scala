package org.blikk.crawler

import com.typesafe.config._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._

/* Convenience accessors for global configuration options */
object Config {

  lazy val config = ConfigFactory.load()

  lazy val requestTimeout = FiniteDuration(
    config.getDuration("blikk.crawler.requestTimeOut", TimeUnit.MILLISECONDS), "ms")
  
  lazy val perDomainDelay = FiniteDuration(
    config.getDuration("blikk.crawler.perDomainDelay", TimeUnit.MILLISECONDS), "ms")

  lazy val customDomainDelays : Map[String, FiniteDuration] = {
    val domainConf = config.getConfig("blikk.crawler.domainDelays")
    val domainKeys = domainConf.entrySet().map(_.getKey)
    domainKeys.map { key =>
      (key, FiniteDuration(domainConf.getDuration(key, TimeUnit.MILLISECONDS), "ms"))
    }.toMap
  }

  lazy val perDomainBuffer = config.getInt("blikk.crawler.perDomainBuffer")

  val ElasticSearchDataExchange = 
    RabbitExchangeDefinition("com.blikk.crawler.data-x-es", "fanout", true, false)


}