package org.blikk.test

import com.redis.RedisClientPool

trait LocalRedis {
  lazy val _localRedis = new RedisClientPool("localhost", 6379)
  implicit def localRedis = _localRedis
}