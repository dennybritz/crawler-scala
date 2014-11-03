package org.blikk.crawler.processors

import akka.stream.scaladsl._
import org.blikk.crawler._
import org.blikk.crawler.app._

object TerminationSink {

  /* Builds a sink that defines termination conditions based on crawl statistics */
  def build(f: CrawlStats => Boolean)(implicit ctx: StreamContext[_]) = {
    
    import ctx.{log, system}

    val zeroStats = CrawlStats(0, 0, System.currentTimeMillis)

    Sink.fold[CrawlStats, CrawlItem](zeroStats) { (currentStats, item) =>
      val newStats = currentStats.update(item)
      if(f(newStats) && !f(currentStats) ) {
        // Shutdown if the termination condition is fulfilled
        log.info("Terminating with: {}", newStats)
        ctx.publisher !  RabbitPublisher.CompleteStream
        system.stop(ctx.publisher)
      }
      newStats
    }
  }

}
