package org.blikk.apps.example

import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.blikk.crawler._
import org.blikk.crawler.app._
import org.blikk.crawler.processors._
import org.jsoup.Jsoup
import scala.util.{Success, Failure}

object Main extends App {
    
  val appName = "com.blikk.example-app"

  // Start a new crawler app
  implicit val system = ActorSystem("appName")
  val app = new CrawlerApp(appName)

  // Create a new stream context
  implicit val streamContext = app.start()
  import streamContext.{materializer, log}
  import system.dispatcher

  /** 
    * Define the flow of the program:
    * - For each incoming page, count the words and aggregate the count
    * - Stop at the crawl after 10 pages
    */
    val seedUrls = List(WrappedHttpRequest.getUrl("http://cnn.com/"))
    val dupFilter = DuplicateFilter.buildUrlDuplicateFilter(seedUrls)
    val frontierSink = FrontierSink.build(streamContext.appId)
    val reqExtractor = RequestExtractor()
    val statusCodeFilter = StatusCodeFilter.build()
    val src = streamContext.flow
    val bcast = Broadcast[CrawlItem]
    val terminationSink = TerminationSink.build {_.numFetched >= 100}

    // Create a processor that counts all words in a document
    val wordCounter = Flow[CrawlItem].map { item =>
      // Use Jsoup to remove all HTML tags
      val soup = Jsoup.parse(item.res.stringEntity.toLowerCase)
      val docText = soup.text()
      // Group by word text
      docText.split(" ").groupBy(identity).mapValues(_.size)
    }

    // Create a processor that aggregates the counts
    val countAggregator = Sink.fold[Map[String, Int], Map[String, Int]](Map.empty) { 
      (counts, newCounts) =>
      counts ++ newCounts.map { case (k,v) => k -> (v + counts.getOrElse(k,0)) }
    }

    val matMap = FlowGraph { implicit b =>
      val frontierMerge = Merge[WrappedHttpRequest]
      // Broadcast all items that were succesfully fetched
      src.buffer(5000, OverflowStrategy.backpressure).via(statusCodeFilter) ~> bcast
      // Exract links and request new URLs from the crawler
      bcast ~> reqExtractor.via(dupFilter) ~> frontierMerge
      // Count words and aggregate
      bcast ~> wordCounter ~> countAggregator
      // Terminate on conditions (more than 10 links fetched)
      bcast ~> terminationSink
      // Logging
      bcast ~> Sink.foreach[CrawlItem]{ item => log.info("processing: {}", item.req.uri.toString) }
      // Iniate the crawl with the seeds
      Source(seedUrls) ~> frontierMerge
      frontierMerge ~> 
        Flow[WrappedHttpRequest].map{ req => log.info("Adding to frontier: {}", req.uri.toString); req } 
        .to(frontierSink)
    }.run()

    // When the stream is over print the result
    matMap.get(countAggregator).onComplete { 
      case Success(finalResult) => 
        val sortedResult = finalResult.toList.sortBy(_._2)
        Console.println(sortedResult.reverse.take(25))
        streamContext.shutdown()
      case Failure(err) => 
        log.error(err.toString)
        streamContext.shutdown()
    }
    
    system.awaitTermination()
}