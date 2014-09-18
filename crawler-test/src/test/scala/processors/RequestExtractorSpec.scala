package org.blikk.test

import akka.stream.scaladsl2._
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
    CrawlItem(WrappedHttpRequest.getUrl(requestUrl), WrappedHttpResponse.withContent(content))
  }

  describe("Stats Collector") {
    
    it("should correctly extract links"){
      val data = List(
        itemWithContent("somesite.com",
          """<a href='http://google.com'>I am a link</a>"""),
        itemWithContent("http://twitter.com",
        """
          <a href='http://twitter.com'>I am a link too.</a>
          <a href='/relative'>I am a link</a>
        """))

      val requestExtractor = RequestExtractor.build()
      val arraySink = FoldSink[List[WrappedHttpRequest], WrappedHttpRequest](Nil)(_ :+ _)
      val flow = FlowFrom(data).append(requestExtractor)
        .withSink(arraySink).run()
      val finalResult = Await.result(arraySink.future(flow), 1.second)

      finalResult.map(_.uri.toString()).toSet shouldBe Set(
        "http://google.com", "http://twitter.com", "http://twitter.com/relative")
      
    }

  }

}