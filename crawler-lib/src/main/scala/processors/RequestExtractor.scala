package org.blikk.crawler.processors

import akka.stream.scaladsl._
import org.blikk.crawler._
import org.jsoup._
import scala.collection.JavaConversions._
import scala.util.Try
import com.google.common.net.InternetDomainName
import com.google.common.net.UrlEscapers
import java.net.URI

object LinkExtractor {
  
  trait LinkSource
  object Content extends LinkSource
  object LocationHeader extends LinkSource

  case class LinkExtraction(destUri: URI, linkSource: LinkSource, sourceUri: URI)

  def apply() : Flow[CrawlItem, List[LinkExtraction]] = {
    Flow[CrawlItem].map { item =>
      val baseUri = item.req.baseUri
      val linkExtractor = new LinkExtractor(baseUri)
      val headerLinks = linkExtractor.extractFromHeaders(item)
      val bodyLinks = linkExtractor.extractFromContent(item)
      (headerLinks ++ bodyLinks)
    }
  }

}

class LinkExtractor(baseUri: String) extends Logging {

  import LinkExtractor._

  val escaper = UrlEscapers.urlFragmentEscaper()
  
  def createEncodedUri(text: String) : Option[URI] = {
    Try(new URI(text)).toOption orElse {
      Try(new URI(escaper.escape(text))).toOption
    } orElse {
      log.warn(s"Could not generate URI from ${text}")
      None
    }
  } 

  

  def extractFromContent(item: CrawlItem) : List[LinkExtraction] = {
    val content = item.res.stringEntity
    val doc = Jsoup.parse(content, baseUri)
    doc.select("a[href]").toList
      .map(_.attr("abs:href").trim())
      .filterNot(_.isEmpty)
      .flatMap(createEncodedUri)
      .map( x => LinkExtraction(x, Content, item.req.uri) )
  }

  def extractFromHeaders(item: CrawlItem) : List[LinkExtraction] = {
    item.res.headers
      .filter(_._1.toLowerCase == "location")
      .map(_._2)
      .flatMap(createEncodedUri)
      .map { uri =>
        val absoluteUri = if (uri.isAbsolute) uri else (new URI(item.req.hostUri + uri.toString))
        LinkExtraction(absoluteUri, LocationHeader, item.req.uri)
      }.toList
  }
}

object SameTPDLinkFilter {

  import LinkExtractor._

  def getTPD(uri: URI) = Try(InternetDomainName.from(uri.getHost.toString)).toOption
    .filter(_.isUnderPublicSuffix)
    .map(_.topPrivateDomain.toString)

  def isSameTPD(a: URI, b: URI) = getTPD(a).getOrElse("") == getTPD(b).getOrElse("")

}


object RequestExtractor {

  import LinkExtractor._

  def apply(internalOnly: Boolean = true) : Flow[CrawlItem, WrappedHttpRequest] = {
    val linkExtractor = LinkExtractor()
    val in = UndefinedSource[CrawlItem]
    val out = UndefinedSink[WrappedHttpRequest]
    val crawlItemBC = Broadcast[CrawlItem]
    val zipper = Zip[CrawlItem, List[LinkExtraction]]

    val sameTPDListFilter = Flow[List[LinkExtraction]].map { links =>
      links.filter { link => link.linkSource match {
        case LocationHeader => true
        case Content => !internalOnly || SameTPDLinkFilter.isSameTPD(link.sourceUri, link.destUri)
      }}
    }

    val requestGenerator = Flow[(CrawlItem, List[LinkExtraction])].mapConcat { case(item, links) =>
      links.map { link => WrappedHttpRequest.getUrl(link.destUri.toString).withProvenance(item.req) }
    }

    Flow[CrawlItem, WrappedHttpRequest]() { implicit b =>
      import FlowGraphImplicits._
      in ~> crawlItemBC
      crawlItemBC ~> zipper.left
      crawlItemBC ~> linkExtractor.via(sameTPDListFilter) ~> zipper.right
      zipper.out ~> requestGenerator ~> out
      in -> out
    }

  }

}