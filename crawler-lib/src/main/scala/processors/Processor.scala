package org.blikk.crawler.processors

import org.blikk.crawler._
import akka.stream._
import akka.stream.scaladsl._
import org.reactivestreams.Subscriber

// trait Processor[A] extends Duct[CrawlItem, A] {
//   def run)
// }

// object Processor {
//   def create(block: (CrawlItem => Unit))(implicit mat: FlowMaterializer) = {
//     Duct[CrawlItem].foreach(block)._1
//   }
// }