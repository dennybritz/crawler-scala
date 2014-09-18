package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.app._

/* Statistics calculated from `CrawlItem` objects */
case class CrawlStats(numFetched: Int, numBytesFetched: Long, startTime: Long) {
  override def toString() = {
    val duration = System.currentTimeMillis - startTime
    s"""numFetched=${numFetched} numBytesFetched=${numBytesFetched} duration=${duration}"""
  }
}

object StatsCollector {

  /* Builds a new stats collector sink */
  def build() = {
    val zeroStats = CrawlStats(0, 0, System.currentTimeMillis)
    FoldSink[CrawlStats, CrawlItem](zeroStats) { (currentStats, item) =>
      CrawlStats(
        currentStats.numFetched + 1,
        currentStats.numBytesFetched + item.res.entity.data.length,
        currentStats.startTime)
    }
  }

}
