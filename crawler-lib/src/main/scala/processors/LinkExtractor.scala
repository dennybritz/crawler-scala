package org.blikk.crawler.processors

import org.blikk.crawler._
import org.blikk.crawler.channels.FrontierChannelInput
import spray.http._
import org.jsoup._
import scala.collection.JavaConversions._

class LinkExtractor(val name: String, filterFunc: Option[(Uri, Uri) => Boolean] = None) 
  extends ResponseProcessor with Logging {

  val MaxProvenance = 10

  def process(res: WrappedHttpResponse, req: WrappedHttpRequest, jobConf: JobConfiguration, 
    context: Map[String, ProcessorOutput]) : Map[String, ProcessorOutput] = {
    // The baseUri is used to resolve relative Uris
    val baseUri = req.uri.scheme + ":" + req.uri.authority + req.uri.path
    // Extract all URLs and create requests
    // Add the current request to the provenance
    val newRequests = getUrls(res.entity.asString, baseUri).toSet[String].map { newUrl  =>
      WrappedHttpRequest.getUrl(newUrl).copy(
        provenance = (req.provenance :+ req.copy(provenance=Nil)).takeRight(MaxProvenance)
      )
    }
    // Optionally Filter the URLs based on the given filter function
    val filteredRequests = filterFunc match {
      case Some(f: ((Uri, Uri) => Boolean)) => newRequests.filter(r => f(req.uri, r.uri))
      case None => newRequests
    }
    Map(name -> FrontierChannelInput(jobConf.jobId, filteredRequests.toSeq))
  }

  def getUrls(htmlString: String, baseUri: String) : List[String] = {
    // We use Jsoup to identify and extract all URLs 
    val doc = Jsoup.parse(htmlString, baseUri)
    doc.select("a[href]").toList.map(_.attr("abs:href")).filterNot(_.isEmpty)
  }


}