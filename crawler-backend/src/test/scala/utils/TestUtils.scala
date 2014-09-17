package org.blikk.test

import com.typesafe.config.ConfigFactory

object TestUtils {

  lazy val testConfig = 
    ConfigFactory.load("application.test")
    .withFallback(ConfigFactory.load())

}