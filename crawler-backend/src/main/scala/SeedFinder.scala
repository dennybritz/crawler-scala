package org.blikk.crawler

import akka.actor.{Address, AddressFromURIString}
import com.typesafe.config.Config

/* Dynamically discovers cluster seed nodes */
trait SeedFinder {
  def findSeeds : List[Address]
}

class LocalSeedFinder(config: Config) extends SeedFinder {

  val systemName = config.getString("blikk.actor-system-name")
  val defaultPort = 8080

  def findSeeds() = List(
    AddressFromURIString.parse(s"akka.tcp://${systemName}@127.0.0.1:${defaultPort}"))
}