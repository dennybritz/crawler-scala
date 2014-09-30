package org.blikk.contactapp

import scala.concurrent.duration._


/* Actor Messages */
/* ================================================== */
trait JobMessage

case class StartJob(
  url: String, 
  callbackUrl: String,
  timeLimit: FiniteDuration) extends JobMessage

case object Shutdown extends JobMessage


/* Model classes */
/* ================================================== */

case class Extraction(value: String, extractionType: String, src: String)
case class Event(payload: String, eventType: String, timestamp: Long)
