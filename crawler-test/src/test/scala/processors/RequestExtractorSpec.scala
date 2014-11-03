package org.blikk.test

import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import org.blikk.crawler.processors.RequestExtractor
import org.blikk.crawler.{CrawlItem, WrappedHttpRequest, WrappedHttpResponse}
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._
import spray.http.{HttpResponse, HttpEntity}

class RequestExtractorSpec extends AkkaSingleNodeSpec("RequestExtractorSpec") {

  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  def itemWithContent(requestUrl: String, content: String) = {
    CrawlItem(WrappedHttpRequest.getUrl(requestUrl), WrappedHttpResponse.withContent(content), "testJob")
  }

  describe("Stats Collector") {
    
    it("should correctly extract internal+external links"){
      val data = List(
        itemWithContent("somesite.com",
          """<a href='http://google.com'>I am a link</a>"""),
        itemWithContent("http://twitter.com",
        """
          <a href='http://twitter.com'>I am a link too.</a>
          <a href='/relative'>I am a link</a>
        """))

      val requestExtractor = RequestExtractor.build(false)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).connect(requestExtractor)
        .runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set(
        "http://google.com", "http://twitter.com", "http://twitter.com/relative")
    }

    it("should correctly extract internal links only"){
      val data = List(
        itemWithContent("somesite.com",
          """<a href='http://google.com'>I am a link</a>"""),
        itemWithContent("http://twitter.com",
        """
          <a href='http://twitter.com'>I am a link too.</a>
          <a href='/relative'>I am a link</a>
          <a href='http://google.com'>
        """))

      val requestExtractor = RequestExtractor.build(true)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).connect(requestExtractor)
        .runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://twitter.com", "http://twitter.com/relative")
    }

  }

}