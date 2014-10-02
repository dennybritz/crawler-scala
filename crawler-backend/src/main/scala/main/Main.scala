package org.blikk.crawler

import akka.actor.{ActorSystem, Props, Address, AddressFromURIString}
import akka.cluster.Cluster
import com.rabbitmq.client.{Connection => RabbitMQConnection, ConnectionFactory => RabbitMQCF}
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.io.Source
import scala.util.{Try, Success, Failure}

object Main extends App with Logging {

  val config = ConfigFactory.load()
  val systemName = config.getString("blikk.actor-system-name")
  val system = ActorSystem(systemName, config)

  // Find the seeds to join the cluster
  val seeds = Try(config.getString("blikk.cluster.seedFile")).toOption match {
    case Some(seedFile) =>
      Source.fromFile(seedFile).getLines.map { address =>
        AddressFromURIString.parse(s"akka.tcp://${systemName}@${address}")
      }.toList
    case None => 
      log.info("No seed file found, using default seeds.")
      val defaultHost = Try(config.getString("blikk.api.host")).toOption
        .getOrElse(InetAddress.getLocalHost.getHostAddress.toString)
      val defaultPort = config.getInt("blikk.api.port")
      List(AddressFromURIString.parse(s"akka.tcp://${systemName}@${defaultHost}:${defaultPort}"))
  }

  log.info(s"Joining cluster with seeds: ${seeds}")
  Cluster.get(system).joinSeedNodes(seeds.toSeq)
  // Start the crawl service and API actors
  val crawlService = system.actorOf(CrawlService.props, "crawl-service")
  val api = system.actorOf(ApiLayer.props(crawlService), "api")
  log.info("crawler ready :)")
  system.awaitTermination()

}