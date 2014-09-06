package org.blikk.crawler

import spray.http._

trait Request extends Serializable {
  def host : String
}

// We use spray.io for HTTP requests
object WrappedHttpRequest {
  
  implicit def sprayConversion(req: HttpRequest) : WrappedHttpRequest = 
    new WrappedHttpRequest(req)
  
  def getUrl(url: String) = 
    new WrappedHttpRequest(new HttpRequest(HttpMethods.GET, Uri(url)))
}

class WrappedHttpRequest(val req: HttpRequest) extends Request {
  def host = req.uri.authority.host.toString
  def port = req.uri.authority.port
}