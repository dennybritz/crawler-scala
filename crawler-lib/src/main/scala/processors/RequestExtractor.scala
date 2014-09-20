package org.blikk.crawler.processors

import akka.stream.scaladsl2.{ProcessorFlow, FlowFrom}
import org.blikk.crawler._
import org.jsoup._
import scala.collection.JavaConversions._

/** 
  * Helper methods for building processes that generate new 
  * requests from fetched content
  */
object RequestExtractor {

  /** 
    * Builds a processor that exractrs links from a given CrawlItem 
    * It returns both extracted links and the original item for provenance
    */
  def buildLinkExtractor() : ProcessorFlow[CrawlItem, (CrawlItem, List[String])] = {
    val extractor = new LinkExtractor()
    FlowFrom[CrawlItem].map { item =>
      val baseUri = item.req.uri.scheme + ":" + item.req.uri.authority + item.req.uri.path
      val links = extractor.extract(item.res.stringEntity, baseUri)
      (item, links)
    }
  }

  def buildRequestGenerator(mapFunc : (CrawlItem, String) => WrappedHttpRequest): 
  ProcessorFlow[(CrawlItem, List[String]), WrappedHttpRequest] = {
    FlowFrom[(CrawlItem, List[String])].mapConcat { case(source, links) =>
      links.map( link => mapFunc(source, link) )
    }
  }

  def buildRequestGenerator() : ProcessorFlow[(CrawlItem, List[String]), WrappedHttpRequest] = 
  buildRequestGenerator { (source, link) =>
    WrappedHttpRequest.getUrl(link).withProvenance(source.req)
  }

  /**
  * Builds a processor that generates new HTTP requests
  * by extracting all links from a fetched document
  */
  def build(mapFunc : (CrawlItem, String) => WrappedHttpRequest) : 
  ProcessorFlow[CrawlItem, WrappedHttpRequest] = {
    val linkExtractor = RequestExtractor.buildLinkExtractor()
    val requestGenerator = RequestExtractor.buildRequestGenerator(mapFunc)
    linkExtractor.append(requestGenerator)
  }

  def build() : ProcessorFlow[CrawlItem, WrappedHttpRequest] = {
    val linkExtractor = RequestExtractor.buildLinkExtractor()
    val requestGenerator = RequestExtractor.buildRequestGenerator()
    linkExtractor.append(requestGenerator)
  }

}

/** 
  Extracts links from a HTML document.
  Internally uses Jsoup to find links and convert relative to absolute URLs.
  */
class LinkExtractor {

  def extract(content: String, baseUri: String) : List[String] = {
    val doc = Jsoup.parse(content, baseUri)
    doc.select("a[href]").toList.map(_.attr("abs:href")).filterNot(_.isEmpty).toList
  }


}