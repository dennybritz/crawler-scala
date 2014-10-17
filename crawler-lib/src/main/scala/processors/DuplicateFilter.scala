package org.blikk.crawler.processors

import akka.stream.scaladsl2.{Flow}
import org.blikk.crawler.WrappedHttpRequest
import com.google.common.hash.{BloomFilter, Funnel, Funnels}

object DuplicateFilter {

  /* Build a duplicate filter based on a string representation of an item */
  def build[A](initialItems : Seq[A] = Nil, 
    expectedInsertions: Int = 1000000, fpp: Double = 0.0001)
  (mapFunc: A => String) : Flow[A,A] = {
    val sdf = new StringDuplicateFilter(expectedInsertions, fpp)
    initialItems.map(mapFunc).foreach(sdf.addItem)
    Flow[A].filter{ item => sdf.filter(mapFunc(item)) }
  }

  /* Builds a duplicate filter based on the URL of the request */
  def buildUrlDuplicateFilter(initialItems : Seq[WrappedHttpRequest] = Nil,
    expectedInsertions: Int = 1000000, fpp: Double = 0.0001) = {
    build[WrappedHttpRequest](initialItems, expectedInsertions, fpp) { req => 
      req.uri.toString
    }
  }

}

/* Filters string duplicates */
class StringDuplicateFilter(val expectedInsertions: Int, val fpp: Double) 
  extends DuplicateFilter[CharSequence] {
  val funnel = Funnels.stringFunnel(java.nio.charset.Charset.defaultCharset)
}


/** 
  * A generic bloom-filter based duplicate eliminator 
  * Sublcasses must provide an appropriate funnel, the expected # of insertions
  * and the false positive probability.
  */
trait DuplicateFilter[A] {

  def expectedInsertions: Int
  def fpp: Double
  def funnel : Funnel[A]
  lazy val bloomFilter : BloomFilter[A] = BloomFilter.create(funnel, expectedInsertions, fpp)

  def filter(item: A) : Boolean = {
    if (bloomFilter.mightContain(item)) {
      false
    } else {
      bloomFilter.put(item)
      true
    }
  }

  /* Manually adds an item to the filter. Useful for initialization. */
  def addItem(item: A) = bloomFilter.put(item)

}