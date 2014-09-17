package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.client._

case class CrawlStats(numFetched: Int, numBytesFetched: Long, duration: Long) {
  override def toString() = {
    s"""crawlstats: numFetched=${numFetched} numBytesFetched=${numBytesFetched} duration=${duration}"""
  }
}

object StatsCollector {

  def build() : ProcessorFlow[CrawlItem, CrawlStats] = {
    val collector = new StatsCollector()
    FlowFrom[CrawlItem].map(collector.run)
  }
}

class StatsCollector {

  val startTime = System.currentTimeMillis
  var currentStats = CrawlStats(0, 0, 0)

  def run(item: CrawlItem) : CrawlStats = {
    currentStats = currentStats.copy(
      numFetched = currentStats.numFetched + 1,
      numBytesFetched = currentStats.numBytesFetched + item.res.entity.data.length,
      duration = System.currentTimeMillis - startTime
    )
    currentStats
  }


}