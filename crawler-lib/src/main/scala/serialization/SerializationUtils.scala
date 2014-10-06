package org.blikk.crawler

import java.io._
import com.google.protobuf._
import com.blikk.serialization._
import scala.collection.JavaConversions._

object SerializationUtils {

  /* CrawlItem */
  /* ================================================== */

  def toProto(crawlItem: CrawlItem) : HttpProtos.CrawlItem = {
    HttpProtos.CrawlItem.newBuilder()
      .setAppId(crawlItem.appId)
      .setReq(toProto(crawlItem.req))
      .setRes(toProto(crawlItem.res))
      .build()
  }

  def fromProto(crawlItem: HttpProtos.CrawlItem) : CrawlItem = {
    CrawlItem(fromProto(crawlItem.getReq), fromProto(crawlItem.getRes), crawlItem.getAppId)
  }

  /* FetchRequest */
  /* ================================================== */

  def toProto(fetchReq: FetchRequest) : HttpProtos.FetchRequest = {
    HttpProtos.FetchRequest.newBuilder()
      .setAppId(fetchReq.appId)
      .setReq(toProto(fetchReq.req))
      .build()
  }

  def fromProto(fetchReq: HttpProtos.FetchRequest) : FetchRequest = {
    FetchRequest(fromProto(fetchReq.getReq), fetchReq.getAppId)
  }

  /* Http Response */
  /* ================================================== */

  def toProto(res: WrappedHttpResponse) : HttpProtos.HttpResponse = {
    val builder = HttpProtos.HttpResponse.newBuilder()
      .setStatusCode(res.status.intValue)
      .setEntity(ByteString.copyFrom(res.entity))
    val headerBuilder = HttpProtos.HttpHeader.newBuilder()
    res.headers.foreach { case (k,v) =>
      builder.addHeaders(headerBuilder.setName(k).setValue(v).build())
    }
    builder.build()
  }

  def fromProto(res: HttpProtos.HttpResponse) : WrappedHttpResponse = {
    WrappedHttpResponse(
      spray.http.StatusCode.int2StatusCode(res.getStatusCode),
      res.getEntity.toByteArray,
      res.getHeadersList.map { h =>
        (h.getName, h.getValue)
      }.toList
    )
  }

  /* Http Request */
  /* ================================================== */

  def toProto(req: WrappedHttpRequest) : HttpProtos.HttpRequest = {
    val builder = HttpProtos.HttpRequest.newBuilder()
      .setMethod(HttpProtos.HttpRequest.HttpMethod.valueOf(req.method))
      .setUri(req.uri.toString)
      .setEntity(ByteString.copyFrom(req.entity))
    val headerBuilder = HttpProtos.HttpHeader.newBuilder()
    req.headers.foreach { case (k,v) =>
      builder.addHeaders(headerBuilder.setName(k).setValue(v).build())
    }
    req.provenance.foreach(builder.addProvenance)
    builder.build()
  }

  def fromProto(req: HttpProtos.HttpRequest) : WrappedHttpRequest = {
    WrappedHttpRequest(
      req.getMethod.toString,
      new java.net.URI(req.getUri),
      req.getHeadersList.map { h =>
        (h.getName, h.getValue)
      }.toList,
      req.getEntity.toByteArray,
      req.getProvenanceList.toList
    )
  }

  /* Generic Serialization */
  /* ================================================== */

  def serialize(obj: Any) : Array[Byte] = {
    val bos = new ByteArrayOutputStream() 
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(obj)
    oos.close()
    bos.toByteArray
  }

  def deserialize[A](bytes: Array[Byte]) = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)
    ois.readObject().asInstanceOf[A]
  }

}