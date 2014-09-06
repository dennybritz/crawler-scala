package org.blikk.test

import akka.routing.BroadcastGroup
import akka.actor._
import akka.testkit._
import org.blikk.crawler._

class TestCrawlService extends CrawlServiceLike with Actor with ActorLogging {
  // For test purpose we simply send all messages to this actor
  // In product this is a consistent hashing router
  val _serviceRouter = context.actorOf(BroadcastGroup(
    List(self.path.toStringWithoutAddress)).props(), 
    "serviceRouter")
  override val serviceRouter = _serviceRouter
  def receive = crawlServiceBehavior
}

class CrawlServiceSpec extends AkkaSingleNodeSpec("CrawlServiceSpec") {

  val jobConf = JobConfiguration.empty("testJob").copy(
    processors = List(new TestResponseProcessor(self)))

  describe("CrawlService") {
    
    describe("Registering an existing job") {
        
        it("should work") {
          val service = TestActorRef[TestCrawlService]
          service.receive(RegisterJob(jobConf))
          service.receive(GetJob("testJob"), self)
          expectMsg(Some(jobConf))
        }
    }

    describe("Routing a request") {
      
      it("should work when the worker does not exist yet") {
        val service = TestActorRef[TestCrawlService]
        service.receive(RegisterJob(jobConf))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        expectMsg("success!")
      }

      it("should work when the worker already exists") {
        val service = TestActorRef[TestCrawlService]
        service.receive(RegisterJob(jobConf))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        expectMsg("success!")
        expectMsg("success!")
        expectMsg("success!")
      }
    }

    describe("Running a new job") {

      it("should work") {
        val service = TestActorRef[TestCrawlService]
        val confWithSeeds= jobConf.copy(seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090"), 
          WrappedHttpRequest.getUrl("http://localhost:9090")))
        service.receive(RunJob(confWithSeeds))
        expectMsg("success!")
        expectMsg("success!")
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        expectMsg("success!")
        expectMsg("success!")
        expectNoMsg()
      }

    }

  }

}