package org.blikk.crawler.processors

import akka.stream.stage._
import akka.stream.scaladsl._
import org.blikk.crawler._

object TerminationSink {

  class TerminationPushTage(f: CrawlStats => Boolean) extends PushStage[CrawlItem, Unit]{
    
    var stats = CrawlStats(0, 0, System.currentTimeMillis)

    override def onPush(element: CrawlItem, ctx: Context[Unit]): Directive = {
      stats = stats.update(element)
      if(f(stats)){
        Console.println("Finishing!")
        ctx.finish()
      }
      else {
        Console.println("Pushing!")
        ctx.push(Unit)
      }
    }

  }

  /* Builds a sink that defines termination conditions based on crawl statistics */
  def build(f: CrawlStats => Boolean) : Sink[CrawlItem] = {
    Flow[CrawlItem].transform(() => new TerminationPushTage(f)).to(Sink.ignore)
  }

}
