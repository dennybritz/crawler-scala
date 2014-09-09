package org.blikk.crawler.channels

import org.blikk.crawler.{JobConfiguration, ProcessorOutput}
import scala.util.control.Exception._

trait OutputChannel[A <: ProcessorOutput] {

  def pipe(input: A, jobConf: JobConfiguration, jobStats: Map[String, Int]) : Unit

  type Closable = { def close() }
  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      ignoring(classOf[Throwable]) apply {
        resource.close()
      }
    }
  }

}