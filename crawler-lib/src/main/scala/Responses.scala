package org.blikk.crawler

trait Response extends Serializable

// We use spray.io for HTTP responses

object WrappedHttpResponse {
  implicit def sprayConversion(res: spray.http.HttpResponse) : WrappedHttpResponse = 
    new WrappedHttpResponse(res)
}

class WrappedHttpResponse(res: spray.http.HttpResponse) extends Response