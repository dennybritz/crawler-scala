package org.blikk.crawler

import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster

object Main extends App with Logging {

  val actorSystemPort = args match {
    case Array(x) => x
    case _ => 2551
  }

  log.debug(s"Starting crawl-service on port=${actorSystemPort}.")

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + actorSystemPort).
    withFallback(ConfigFactory.load())
  val system = ActorSystem("blikk-crawl-service", config)

  // Find the seed nodes
  // TODO: Right now this is only done locally
  Cluster.get(system).joinSeedNodes((new LocalSeedFinder()).findSeeds())
  // Start a service actor
  system.actorOf(Props[CrawlService], name = "crawlService")
  system.awaitTermination()

}