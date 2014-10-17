package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.app._

object StatsCollector {

  /* Builds a new stats collector sink */
  def build() = {
    val zeroStats = CrawlStats(0, 0, System.currentTimeMillis)
    FoldDrain[CrawlStats, CrawlItem](zeroStats) { (currentStats, item) =>
      currentStats.update(item)
    }
  }

}
