package org.blikk.crawler.processors

import akka.stream.scaladsl2.{ProcessorFlow, FlowFrom}
import org.blikk.crawler._
import org.jsoup._
import scala.collection.JavaConversions._
import scala.util.Try

/** 
  * Helper methods for building processes that generate new 
  * requests from fetched content
  */
object RequestExtractor extends Logging {

  /** 
    * Builds a processor that exractrs links from a given CrawlItem 
    * It returns both extracted links and the original item for provenance
    */
  def buildLinkExtractor() : ProcessorFlow[CrawlItem, (CrawlItem, Set[String])] = {
    val extractor = new LinkExtractor()
    FlowFrom[CrawlItem].map { item =>
      val baseUri = item.req.uri.scheme + ":" + item.req.uri.authority + item.req.uri.path
      // We additionaly extract the `location` header used for redirects
      val redirectUrls = item.res.headers.filter(_._1.toLowerCase == "location").map(_._2).toSet
      val links = extractor.extract(item.res.stringEntity, baseUri) ++ redirectUrls
      val normalizedLinks = links.map(UrlNormalizer.normalize)
      (item, normalizedLinks)
    }
  }

  def buildRequestGenerator(internalOnly: Boolean = true)
    (mapFunc : (CrawlItem, String) => WrappedHttpRequest) : 
    ProcessorFlow[(CrawlItem, Set[String]), WrappedHttpRequest] = {
      FlowFrom[(CrawlItem, Set[String])].mapConcat { case(source, links) =>
        links.flatMap { link => 
          Try(mapFunc(source, link)).toOption orElse { 
            log.warn(s"Could not generate request from ${link}")
            None
          }
        }.filterNot { newReq =>
          internalOnly && (newReq.topPrivateDomain != source.req.topPrivateDomain)
        }.toList
      }
  }

  def build(internalOnly: Boolean, mapFunc : (CrawlItem, String) => WrappedHttpRequest) : 
  ProcessorFlow[CrawlItem, WrappedHttpRequest] = {
    val linkExtractor = RequestExtractor.buildLinkExtractor()
    val requestGenerator = RequestExtractor.buildRequestGenerator(internalOnly)(mapFunc)
    linkExtractor.append(requestGenerator)
  }

  /**
    * Builds a processor that generates new HTTP requests
    * by extracting all links from a fetched document
    */
  def build(internalOnly: Boolean = true) : ProcessorFlow[CrawlItem, WrappedHttpRequest] = {
    val linkExtractor = RequestExtractor.buildLinkExtractor()
    val requestGenerator = RequestExtractor.buildRequestGenerator(internalOnly)((source, link) =>
      WrappedHttpRequest.getUrl(link).withProvenance(source.req))
    linkExtractor.append(requestGenerator)
  }

}

/** 
  Extracts links from a HTML document.
  Internally uses Jsoup to find links and convert relative to absolute URLs.
  */
class LinkExtractor {

  def extract(content: String, baseUri: String) : Set[String] = {
    val doc = Jsoup.parse(content, baseUri)
    doc.select("a[href]").toList.map(_.attr("abs:href").trim()).filterNot(_.isEmpty).toSet
  }


}