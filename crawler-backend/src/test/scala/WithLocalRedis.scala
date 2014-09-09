package org.blikk.test

import com.redis.RedisClient

trait LocalRedis {
  lazy val _localRedis = new RedisClient("localhost", 6379)
  implicit def localRedis = _localRedis
}