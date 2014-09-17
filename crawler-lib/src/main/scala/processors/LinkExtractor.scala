package org.blikk.crawler.processors

import akka.stream.scaladsl2._
import org.blikk.crawler._
import org.blikk.crawler.client._
import org.jsoup._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import spray.http.Uri

object LinkExtractor {
  def noFilter() : ProcessorFlow[CrawlItem, WrappedHttpRequest] = {
    val extractor = new LinkExtractor((_, _) => true)
    FlowFrom[CrawlItem].mapConcat(extractor.run)
  }
}

class LinkExtractor(filterFunc: (Uri, Uri) => Boolean) {

  def run(item: CrawlItem) : List[WrappedHttpRequest] = {
    // The baseUri is used to resolve relative Uris
    val baseUri = item.req.uri.scheme + ":" + item.req.uri.authority + item.req.uri.path
    // Extract all URLs and create requests. Filter them.
    getUrls(item.res.entity.asString, baseUri).toSet[String].map { newUrl  =>
      WrappedHttpRequest.getUrl(newUrl).withProvenance(item.req)
    }.filter { req => filterFunc(item.req.uri, req.uri) }.toList
  }

  // We use Jsoup to identify and extract all URLs 
  def getUrls(htmlString: String, baseUri: String) : List[String] = {
    val doc = Jsoup.parse(htmlString, baseUri)
    doc.select("a[href]").toList.map(_.attr("abs:href")).filterNot(_.isEmpty)
  }

}