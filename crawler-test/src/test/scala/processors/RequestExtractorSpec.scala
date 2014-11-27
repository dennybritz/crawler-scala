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

  def itemWithLocationHeader(requestUrl: String, locationUri: String) = {
    CrawlItem(WrappedHttpRequest.getUrl(requestUrl), 
      WrappedHttpResponse.empty().copy(headers=List(("location",locationUri))),
      "testJob")
  }

  def itemWithContent(requestUrl: String, content: String) = {
    CrawlItem(WrappedHttpRequest.getUrl(requestUrl), WrappedHttpResponse.withContent(content), "testJob")
  }

  describe("RequestExtractor") {
    
    it("should correctly extract internal+external links"){
      val data = List(
        itemWithContent("somesite.com",
          """<a href='http://google.com'>I am a link</a>"""),
        itemWithContent("http://twitter.com",
        """
          <a href='http://twitter.com'>I am a link too.</a>
          <a href='/relative'>I am a link</a>
        """))

      val requestExtractor = RequestExtractor(false)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
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

      val requestExtractor = RequestExtractor(true)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://twitter.com", "http://twitter.com/relative")
    }

    it("shold be able to handle URLs with spaces and other special characters") {
      val data = List(
        itemWithContent("http://somesite.com/",
          """<a href='http://somesite.com/I am a link/with'>I am a link with spaces</a>"""))

      val requestExtractor = RequestExtractor(true)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://somesite.com/I%20am%20a%20link/with")
    }

    it("shold be able to handle URLs that are already escaped") {
      val data = List(
        itemWithContent("http://somesite.com/",
          """<a href='http://somesite.com/I%20am%20a%20link/with'>I am a link with spaces</a>"""))

      val requestExtractor = RequestExtractor(true)
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://somesite.com/I%20am%20a%20link/with")
    }

    it("should extract absolute links from location headers") {
      val data = List(
        itemWithLocationHeader("http://somesite.com", "http://www.somesite.com"),
        itemWithLocationHeader("http://somesite.com", "http://someothersite.com")
      )

      val requestExtractor = RequestExtractor()
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://www.somesite.com", "http://someothersite.com")
    }

    it("should extract relative links from location headers") {
      val data = List(
        itemWithLocationHeader("http://somesite.com", "/start"),
         itemWithLocationHeader("http://somesite.com", "/start with spaces")
      )

      val requestExtractor = RequestExtractor()
      val arraySink = Sink.fold[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val resultFuture = Source(data).via(requestExtractor).runWith(arraySink)
      val finalResult = Await.result(resultFuture, 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set("http://somesite.com/start", "http://somesite.com/start%20with%20spaces")
    }

  }

}