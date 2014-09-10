package org.blikk.crawler

object Resource {

  type Closable = { def close() }
  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

}
