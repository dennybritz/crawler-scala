package org.blikk.crawler.processors


// import org.blikk.crawler._
// import org.blikk.crawler.channels.FrontierChannelInput
// import org.blikk.crawler.channels.FrontierChannelInput.AddToFrontierRequest
// import spray.http._
// import org.jsoup._
// import scala.collection.JavaConversions._

// class LinkExtractor(val name: String, filterFunc: Option[(Uri, Uri) => Boolean] = None) 
//   extends ResponseProcessor with Logging {

//   val MaxProvenance = 10

//   def process(in: ResponseProcessorInput) : Map[String, ProcessorOutput] = {
//     // The baseUri is used to resolve relative Uris
//     val baseUri = in.req.uri.scheme + ":" + in.req.uri.authority + in.req.uri.path
//     // Extract all URLs and create requests
//     // Add the current request to the provenance
//     val newRequests = getUrls(in.res.entity.asString, baseUri).toSet[String].map { newUrl  =>
//       WrappedHttpRequest.getUrl(newUrl).withProvenance(in.req)
//     }
//     // Optionally Filter the URLs based on the given filter function
//     val filteredRequests = filterFunc match {
//       case Some(f: ((Uri, Uri) => Boolean)) => newRequests.filter(r => f(in.req.uri, r.uri))
//       case None => newRequests
//     }
//     val result = FrontierChannelInput(filteredRequests.toSeq.map( req => AddToFrontierRequest(req)))
//     Map(name -> result)
//   }

//   def getUrls(htmlString: String, baseUri: String) : List[String] = {
//     // We use Jsoup to identify and extract all URLs 
//     val doc = Jsoup.parse(htmlString, baseUri)
//     doc.select("a[href]").toList.map(_.attr("abs:href")).filterNot(_.isEmpty)
//   }


// }