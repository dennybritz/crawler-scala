package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.app._
import scala.concurrent.Future
import spray.http.Uri
import com.google.common.hash.{BloomFilter, Funnels}

object UrlDuplicateFilter {
  def build(expectedInsertions: Int = 1000000, fpp: Double = 0.0001) : 
  ProcessorFlow[WrappedHttpRequest, WrappedHttpRequest] =  {
    val filter = new UrlDuplicateFilter(expectedInsertions, fpp)
    FlowFrom[WrappedHttpRequest].filter(filter.runThrough)
  }
}

/* A Bloom-filter based url duplicate eliminator */
class UrlDuplicateFilter(expectedInsertions: Int = 1000000, fpp: Double = 0.0001) {
  /* Bloom filter properties */
  val funnel = Funnels.stringFunnel(java.nio.charset.Charset.defaultCharset)
  val bloomFilter = BloomFilter.create(funnel, expectedInsertions, fpp)

  /* Run and item through and adds to the filter if necessary */
  def runThrough(item: WrappedHttpRequest) : Boolean = {
    bloomFilter.mightContain(item.req.uri.toString) match {
      case true => false
      case false => 
        bloomFilter.put(item.req.uri.toString)
        true
    }
  }

}
