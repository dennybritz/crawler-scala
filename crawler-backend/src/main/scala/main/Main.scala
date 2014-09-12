package org.blikk.crawler

import scala.io.Source
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, Props, Address, AddressFromURIString}
import akka.cluster.Cluster
import com.redis.RedisClientPool
import scala.util.{Try, Success, Failure}

object Main extends App with Logging {

  val config = ConfigFactory.load()
  val systemName = config.getString("blikk.actor-system-name")
  val system = ActorSystem(systemName, config)

  // Initialize a redis client pool
  val redisHost = config.getString("blikk.redis.host")
  val redisPort = config.getInt("blikk.redis.port")
  val redisPrefix = config.getString("blikk.redis.prefix")
  val localRedis = new RedisClientPool(redisHost, redisPort)

  // Make sure redis is running
  Try(localRedis.withClient { client =>
    client.set("blikk-crawler:test", "hello")
  }) match {
    case Failure(err) => 
      log.error("Redis is not running?", err)
      System.exit(1)
    case _ => // OK
  }

  // Find the seeds to join the cluster
  val seeds = Try(config.getString("blikk.cluster.seedFile")).toOption match {
    case Some(seedFile) =>
      Source.fromFile(seedFile).getLines.map { address =>
        AddressFromURIString.parse(s"akka.tcp://${systemName}@${address}")
      }.toList
    case None => 
      log.info("No seed file found, using default seeds.")
      val defaultPort = config.getInt("blikk.api.port")
      List(AddressFromURIString.parse(s"akka.tcp://${systemName}@127.0.0.1:${defaultPort}"))
  }

  log.info(s"Joining cluster with seeds: ${seeds}")
  Cluster.get(system).joinSeedNodes(seeds.toSeq)
  // Start the crawl service and API actors
  val crawlService = system.actorOf(CrawlService.props(localRedis, redisPrefix), "crawl-service")
  val api = system.actorOf(ApiLayer.props(crawlService), "api")
  log.info("crawler ready :)")
  system.awaitTermination()

}