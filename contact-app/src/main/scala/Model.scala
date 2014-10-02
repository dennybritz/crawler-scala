package org.blikk.contactapp

import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._


/* Actor Messages */
/* ================================================== */
trait JobMessage

case class StartJob(
  url: String,
  appId: String,
  timeLimit: FiniteDuration) extends JobMessage

case object Shutdown extends JobMessage


/* Model classes */
/* ================================================== */

case class Extraction(value: String, extractionType: String, src: String)
case class Event(payload: String, eventType: String, timestamp: Long)
case class Request(url: String, appId: String)

object JsonProtocols {
  implicit val extractionFormat = jsonFormat3(Extraction)
  implicit val requestFormat = jsonFormat2(Request)
  implicit val eventFormat = jsonFormat3(Event)
}