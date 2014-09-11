package org.blikk.test

import com.redis.RedisClientPool

trait LocalRedis {

  val redisHost = TestUtils.testConfig.getString("blikk.redis.host")
  val redisPort = TestUtils.testConfig.getInt("blikk.redis.port")

  implicit lazy val localRedis = new RedisClientPool(redisHost, redisPort)
}