package org.blikk.crawler

import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.redis.RedisClientPool

object Main extends App with Logging {

  val config = ConfigFactory.load()
  val system = ActorSystem("blikk-crawler", config)

  // Initialize a redis client pool
  val redisHost = config.getString("blikk.redis.host")
  val redisPort = config.getInt("blikk.redis.port")
  val redisPrefix = config.getString("blikk.redis.prefix")
  val localRedis = new RedisClientPool(redisHost, redisPort)

  localRedis.withClient { client =>
    client.set("blikk-crawler:test", "hello")
    client.get("blikk-crawler:test") match {
      case Some("hello") => // OK
      case _ => log.error("redis not working correctly. set/get key test was not successful.") 
    }
  }

  // Find the seed nodes
  // TODO: Right now this is only done locally
  val seedFinder = new LocalSeedFinder(config)
  Cluster.get(system).joinSeedNodes(seedFinder.findSeeds())
  // Start the crawl service and API actors
  val crawlService = system.actorOf(CrawlService.props(localRedis, redisPrefix), "crawl-service")
  val api = system.actorOf(ApiLayer.props(crawlService), "api")
  log.info("crawler ready :)")
  system.awaitTermination()

}