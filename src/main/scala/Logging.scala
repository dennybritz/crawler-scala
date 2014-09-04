package org.blikk.crawler

trait Logging {

  lazy val log = org.slf4j.LoggerFactory.getLogger(this.getClass.getName)

}