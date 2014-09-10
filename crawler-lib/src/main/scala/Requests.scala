package org.blikk.crawler

import spray.http._
import java.util.UUID

trait Request extends Serializable

// We use spray.io for HTTP requests
object WrappedHttpRequest {
  
  implicit def sprayConversion(req: HttpRequest) : WrappedHttpRequest = 
    new WrappedHttpRequest(req)
  implicit def sprayConversion(req: WrappedHttpRequest) : spray.http.HttpRequest = 
    req.req
  
  def getUrl(url: String) = 
    new WrappedHttpRequest(new HttpRequest(HttpMethods.GET, Uri(url)))
}

case class WrappedHttpRequest(req: HttpRequest, 
  timestamp : Long,
  provenance: List[WrappedHttpRequest] = List.empty,
  uuid : String = UUID.randomUUID.toString()) extends Request {
  def this(req: HttpRequest) = this(req, System.currentTimeMillis)
  def host = req.uri.authority.host.toString
  def port = req.uri.authority.port

  def withProvenance(source: WrappedHttpRequest, maxProvenance : Int = 10) : 
    WrappedHttpRequest = {
    this.copy(
      provenance = (source.provenance :+ source.copy(provenance=Nil)).takeRight(maxProvenance)
    )
  }

}