package org.blikk.crawler.processors

import org.blikk.crawler.CrawlItem

/* Statistics calculated from `CrawlItem` objects */
case class CrawlStats(numFetched: Int, numBytesFetched: Long, startTime: Long) {
  
  /* Returns an updates `CrawlStats` instance */
  def update(item: CrawlItem) : CrawlStats = {
     CrawlStats(
        this.numFetched + 1,
        this.numBytesFetched + item.res.stringEntity.getBytes.length,
        this.startTime)
  }

  override def toString() = {
    val duration = System.currentTimeMillis - startTime
    s"""numFetched=${numFetched} numBytesFetched=${numBytesFetched} duration=${duration}"""
  }

}
