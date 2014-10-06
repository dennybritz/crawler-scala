package org.blikk.crawler

import spray.http._
import com.google.common.net.InternetDomainName
import com.blikk.serialization._
import scala.util.Try

object WrappedHttpRequest {
  
  implicit def fromSpray(req: HttpRequest) = 
    WrappedHttpRequest(
      req.method.toString,
      new java.net.URI(req.uri.toString),
      req.headers.map(HttpHeader.unapply).map(_.get),
      req.entity.data.toByteArray
    )

  implicit def toSpray(req: WrappedHttpRequest) =
    HttpRequest(
      HttpMethods.getForKey(req.method).get,
      Uri(req.uri.toString),
      req.headers.map { case(k,v) => HttpHeaders.RawHeader(k,v) },
      HttpEntity(req.entity)
    )
  
  /* Builds a new wrapped HTTP request with method GET for the given url */
  def getUrl(url: String) = 
    WrappedHttpRequest("GET", new java.net.URI(url), Nil, Array.empty[Byte])

  def empty =  WrappedHttpRequest("GET", new java.net.URI(""), Nil, Array.empty[Byte])
}

/* A HTTP request with additional fields */
case class WrappedHttpRequest(
  method: String,
  uri: java.net.URI,
  headers: List[(String, String)],
  entity: Array[Byte],
  provenance: List[String] = List.empty) extends java.io.Serializable {

  def host = Try(uri.getHost.toString).getOrElse("")
  def port = Try(uri.getPort).getOrElse(80)
  
  lazy val baseUri = new java.net.URI(uri.getScheme, uri.getUserInfo, uri.getHost, uri.getPort, 
      uri.getPath, null, null).toString
  
  lazy val topPrivateDomain = Try(InternetDomainName.from(host)).toOption
    .filter(_.isUnderPublicSuffix)
    .map(_.topPrivateDomain.toString)

  lazy val publicSuffix = topPrivateDomain.map(InternetDomainName.from)
    .map(_.parent.toString)

  /** 
    * Copies this request with the source request appended in the provenance list 
    * Cuts off the provenance at `maxProvenance` items.
    */
  def withProvenance(source: WrappedHttpRequest, maxProvenance : Int = 10) = {
    val newProvenance = (source.provenance :+ source.uri.toString).takeRight(maxProvenance)
    this.copy(provenance = newProvenance)
  }

}