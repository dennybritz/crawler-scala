package org.blikk.crawler

import akka.actor.{Address, AddressFromURIString}

/* Dynamically discovers cluster seed nodes */
trait SeedFinder {
  def findSeeds : List[Address]
}

class LocalSeedFinder extends SeedFinder {
  def findSeeds() = List(
    AddressFromURIString.parse("akka.tcp://blikk-crawl-service@127.0.0.1:2551"))
}