package org.blikk.test

import com.typesafe.config.ConfigFactory

object TestConfig {

  lazy val config = ConfigFactory.load("application.test").withFallback(ConfigFactory.load)
  
  lazy val RabbitMQUri = config.getString("blikk.rabbitMQ.uri")
  lazy val HttpServerPort = config.getInt("blikk.testHttpServer.port")
  lazy val HttpServerHost = config.getString("blikk.testHttpServer.host")

}