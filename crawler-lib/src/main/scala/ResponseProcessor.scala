package org.blikk.crawler

import com.typesafe.config.Config

/* Processors define their own processor output. The backend must know how to handle these. */
trait ProcessorOutput extends Serializable

trait ResponseProcessor extends Serializable {
  def name : String
  def process(in: ResponseProcessorInput) : Map[String, ProcessorOutput]
}

case class ResponseProcessorInput(
  res: WrappedHttpResponse, 
  req: WrappedHttpRequest, 
  jobConf: JobConfiguration,
  jobStats: Map[String, Int] = Map.empty,
  context: Map[String, ProcessorOutput] = Map.empty
)