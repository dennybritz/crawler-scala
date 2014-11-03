package org.blikk.crawler.processors

import akka.stream.scaladsl._
import org.blikk.crawler._
import org.blikk.crawler.app._

object StatsCollector {

  /* Builds a new stats collector sink */
  def build() = {
    val zeroStats = CrawlStats(0, 0, System.currentTimeMillis)
    Sink.fold[CrawlStats, CrawlItem](zeroStats) { (currentStats, item) =>
      currentStats.update(item)
    }
  }

}
