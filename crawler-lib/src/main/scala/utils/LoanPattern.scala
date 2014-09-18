package org.blikk.crawler

object Resource {

  type Closable = { def close() }

  /* Uses a closable resources in a block and closes it afterwards */
  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

}
