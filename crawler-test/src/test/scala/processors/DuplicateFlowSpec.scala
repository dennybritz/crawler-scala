package org.blikk.test

import akka.actor._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import org.blikk.crawler._
import org.blikk.crawler.processors.{PersistentDuplicateFilter}
import org.scalatest._
import akka.testkit._

class DuplicateFilterFlowSpec extends AkkaSingleNodeSpec("DuplicateFilterFlowSpec") {

  import system.dispatcher
  implicit val materializer = FlowMaterializer(akka.stream.MaterializerSettings(system))

  var pdfActor : ActorRef = _

  describe("DuplicateFilterFlow") {

    before {
      pdfActor = system.actorOf(PersistentDuplicateFilter.props("testDuplicateFilterFlow"))
      watch(pdfActor)
    }

    after {
      pdfActor ! PersistentDuplicateFilter.DeleteMessages
      pdfActor ! PersistentDuplicateFilter.DeleteSnapshots
      pdfActor ! PersistentDuplicateFilter.Shutdown
      expectMsgClass(classOf[Terminated])
    }

    it("should work") {
      val testProbe = TestProbe()
      val data = List("item1", "item2", "item1", "item3", "item2")
      val duplicateFilter = PersistentDuplicateFilter.flow[String](pdfActor)
      Source(data).via(duplicateFilter).runWith(ForeachSink { item => testProbe.ref ! item })
      testProbe.expectMsg("item1")
      testProbe.expectMsg("item2")
      testProbe.expectMsg("item3")
      testProbe.expectNoMsg()
    }

  }

}