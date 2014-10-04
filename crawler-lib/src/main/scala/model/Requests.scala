package org.blikk.crawler

import spray.http.{HttpRequest, HttpMethods, Uri}
import com.google.common.net.InternetDomainName
import scala.util.Try

object WrappedHttpRequest {
  
  implicit def fromSpray(req: HttpRequest) = new WrappedHttpRequest(req)
  implicit def toSpray(wrappedReq: WrappedHttpRequest) = wrappedReq.req
  
  /* Builds a new wrapped HTTP request with method GET for the given url */
  def getUrl(url: String) = 
    WrappedHttpRequest(new HttpRequest(HttpMethods.GET, Uri(url)))

  def empty = WrappedHttpRequest(new HttpRequest())
}

/* A HTTP request with additional fields */
case class WrappedHttpRequest(req: HttpRequest, 
  provenance: List[WrappedHttpRequest] = List.empty) {

  def host = req.uri.authority.host.toString
  def port = req.uri.authority.port
  
  lazy val topPrivateDomain = Try(InternetDomainName.from(host)).toOption
    .filter(_.isUnderPublicSuffix)
    .map(_.topPrivateDomain.toString)

  lazy val publicSuffix = topPrivateDomain.map(InternetDomainName.from)
    .map(_.parent.toString)

  lazy val reverseHost = host.split('.').reverse.mkString(".")

  /** 
    * Copies this request with the source request appended in the provenance list 
    * Cuts off the provenance at `maxProvenance` items.
    */
  def withProvenance(source: WrappedHttpRequest, maxProvenance : Int = 10) = {
    val newProvenance = (source.provenance :+ source.copy(provenance=Nil)).takeRight(maxProvenance)
    this.copy(provenance = newProvenance)
  }

}