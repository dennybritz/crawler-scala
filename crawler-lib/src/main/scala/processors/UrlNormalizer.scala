package org.blikk.crawler.processors

/** 
  * Helper class to normalize URLs
  * It does:
  * - Strip trailing # anchors
  * - Strip trailing slashes
  */
object UrlNormalizer {

  def normalize(url: String) : String = {
    val transformations = List[(String => String)](
      stripHashSuffix, stripTrailingSlash)
    transformations.foldLeft(url) { (url, f) => f(url) }
  }
    

  def stripHashSuffix(url: String) : String = {
    url.lastIndexOf('#') match {
      case x if x > -1 => url.slice(0, x)
      case _ => url
    }
  }

  def stripTrailingSlash(url: String) : String = {
    if (url.endsWith("/")) url.slice(0, url.size-1) else url
  }

}