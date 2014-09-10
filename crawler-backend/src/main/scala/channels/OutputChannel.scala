package org.blikk.crawler.channels

import org.blikk.crawler.{JobConfiguration, ProcessorOutput}
import scala.util.control.Exception._

trait OutputChannel[A <: ProcessorOutput] {

  def pipe(input: A, jobConf: JobConfiguration, jobStats: Map[String, Int]) : Unit

}