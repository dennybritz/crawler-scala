package org.blikk.test

import akka.actor._
import akka.testkit._
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll}
import org.blikk.crawler._
import scala.concurrent.duration._


class HostWorkerSpec extends AkkaSingleNodeSpec("HostWorkerSpec") {

  val testProcessor = new TestResponseProcessor(self)

  describe("HostWorker") {
    
    it("should work with a simple processor") {
      val parentProbe = TestProbe()
      val jobConf = JobConfiguration.empty("testJob").copy(processors = List(testProcessor))
      val actor = TestActorRef(HostWorker.props(parentProbe.ref), parentProbe.ref, "worker")
      actor.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
      parentProbe.expectMsg(GetJob("testJob"))
      parentProbe.reply(Some(jobConf))
      expectMsg("http://localhost:9090")
    }

    it("should not process the response if it cannot find the job configuration") {
      val parentProbe = TestProbe()
      val jobConf = JobConfiguration.empty("testJob").copy(processors = List(testProcessor))
      val actor = TestActorRef(HostWorker.props(parentProbe.ref), parentProbe.ref, "worker")
      actor.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
      parentProbe.expectMsg(GetJob("testJob"))
      parentProbe.reply(None)
      expectNoMsg(500.millis)
    }

    it("should work with a chain of processors") {
      val parentProbe = TestProbe()
      def jobConf = JobConfiguration.empty("testJob").copy(
        processors = List(testProcessor, testProcessor, testProcessor))
      val actor = TestActorRef(HostWorker.props(parentProbe.ref), parentProbe.ref, "worker")
      actor.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
      parentProbe.expectMsg(GetJob("testJob"))
      parentProbe.reply(Some(jobConf))
      expectMsg("http://localhost:9090")
      expectMsg("http://localhost:9090")
      expectMsg("http://localhost:9090")
    }
  }


}