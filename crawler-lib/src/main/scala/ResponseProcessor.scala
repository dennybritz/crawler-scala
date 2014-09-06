package org.blikk.crawler

trait ResponseProcessor extends Serializable {
  def name : String
  def process(
    res: WrappedHttpResponse, 
    req: WrappedHttpRequest, 
    jobConf: JobConfiguration,
    context: Map[String, Any]) : Map[String, Any]
}
