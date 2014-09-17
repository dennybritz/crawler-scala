package org.blikk.crawler

/* An item that is delivered from the crawler*/
case class CrawlItem(req: WrappedHttpRequest, res: WrappedHttpResponse)