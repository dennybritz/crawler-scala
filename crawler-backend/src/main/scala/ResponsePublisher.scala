package org.blikk.crawler

import akka.actor._
import akka.stream.actor._
import spray.http.{HttpHeaders, HttpHeader}

/** 
  * Receives FetchResponse and produces a stream.
  * This actor only exists so that we can generate a Crawl stream with `Source`.
  * It does not contain any domain logic.
  */
class ResponsePublisher extends Actor with ActorLogging 
  with ActorPublisher[FetchResponse] {

  override def preStart(){
    log.info("starting")
  }

  def receive = {
    case msg : FetchResponse =>  processItem(msg)
    case msg : ActorPublisherMessage => // Nothing to do
    case msg => log.warning("unhandled message: {}", msg) 
  }

  def processItem(msg: FetchResponse) {

    // Logging interesting stuff :)
    val interestingHeaders = List(
      HttpHeaders.`Content-Length`,
      HttpHeaders.`Content-Type`,
      HttpHeaders.`Transfer-Encoding`,
      HttpHeaders.`Content-Encoding`
    ).map(_.lowercaseName);

    val headerLog = msg.res.headers
      .filter { case(name, value) => interestingHeaders.contains(name.toLowerCase) }
      .map { case(name, value) =>  s"""${name}="${value}" """ }
      .mkString(" ")

    log.info(log.format("Fetched url=\"{}\" tpd=\"{}\" status=\"{}\", size=\"{}\" {}",
      msg.fetchReq.req.uri.toString,
      msg.fetchReq.req.topPrivateDomain,
      msg.res.status.value,
      msg.res.entity.length.toString,
      headerLog))
    if (isActive && totalDemand > 0) {
      onNext(msg)
    } else {
      // TODO: This is ugly, should probably keep a buffer
      log.warning("demand is too low, requeuing message")
      self ! msg
    }
  }

}