package org.blikk.crawler

import scala.util.Try

trait ResponseProcessor extends Serializable {
  def name : String
  def process(
    res: WrappedHttpResponse, 
    req: WrappedHttpRequest, 
    jobConf: JobConfiguration,
    context: Map[String, Any]) : Map[String, Any]
}
