package org.blikk.crawler

object Resource extends Logging {

  type Closable = { def close() }

  /* Uses a closable resources in a block and closes it afterwards */
  def using[R <: Closable, A <: Any](resource: R)(f: R => A): Option[A] = {
    try {
      Option(f(resource))
    } catch { case e: java.io.IOException =>
      log.error(e.toString)
      return None
    } finally {
      resource.close()
    }
  }

}
