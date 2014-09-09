package org.blikk.test

import com.redis.RedisClient
import akka.routing.{Broadcast, AddRoutee, ActorRefRoutee, Routee, ConsistentHashingGroup}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.actor._
import akka.testkit._
import org.blikk.crawler._


object TestCrawlService {
  def props(implicit localRedis: RedisClient) = Props(classOf[TestCrawlService], localRedis)
}

class TestCrawlService(val localRedis: RedisClient) extends CrawlServiceLike with Actor with ActorLogging {
  // For test purpose we simply send all messages to this actor
  // In product this is a consistent hashing router
  val _serviceRouter = context.actorOf(ConsistentHashingGroup(
    List(self.path.toStringWithoutAddress)).props(), 
    "serviceRouter")
  override val serviceRouter = _serviceRouter
  def extraBehavior : Receive = {
    case msg : AddRoutee =>
      _serviceRouter ! msg
  }

  def receive = extraBehavior orElse defaultBehavior
  val jobStatsCollector = context.actorOf(Props[JobStatsCollector], "jobStatsCollector")

}

class CrawlServiceSpec extends AkkaSingleNodeSpec("CrawlServiceSpec") {

  val jobConf = JobConfiguration.empty("testJob").copy(
    processors = List(new TestResponseProcessor(self)))

  describe("CrawlService") {
    
    describe("Registering an existing job") {
        
        it("should work") {
          val service = TestActorRef(TestCrawlService.props, "crawlService1")
          service.receive(RegisterJob(jobConf))
          service.receive(GetJob("testJob"), self)
          expectMsg(Some(jobConf))
          service.stop()
        }
    }

    describe("Routing a request") {
      
      it("should work when the worker does not exist yet") {
        val service = TestActorRef(TestCrawlService.props, "crawlService2")
        service.receive(RegisterJob(jobConf))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
        expectMsg("http://localhost:9090")
        service.stop()
      }

      it("should work with multiple requests to the same host") {
        val service = TestActorRef(TestCrawlService.props, "crawlService3")
        service.receive(RegisterJob(jobConf))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/1"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/2"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob"))
        receiveN(3).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2", "http://localhost:9090/3")
        service.stop()
      }
    }

    describe("Running a new job with multiple requests") {

      it("should work") {
        val service = TestActorRef(TestCrawlService.props, "crawlService4")
        val confWithSeeds= jobConf.copy(seeds = List(WrappedHttpRequest.getUrl("http://localhost:9090/1"), 
          WrappedHttpRequest.getUrl("http://localhost:9090/2")))
        service.receive(RunJob(confWithSeeds))
        receiveN(2).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2")
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/4"), "testJob"))
        receiveN(2).toSet == Set("http://localhost:9090/3", "http://localhost:9090/4")
        expectNoMsg()
        service.stop()
      }
    }

    describe("Getting global job statistics") {

      it("should work") {
        val service = TestActorRef(TestCrawlService.props, "crawlService5")
        val service2 = TestActorRef(TestCrawlService.props, "crawlService6")
        
        service.receive(AddRoutee(ActorRefRoutee(service2)))
        service2.receive(AddRoutee(ActorRefRoutee(service)))
        service.receive(RegisterJob(jobConf))
        service2.receive(RegisterJob(jobConf))

        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/1"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/2"), "testJob"))
        service2.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob"))
        receiveN(3).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2", "http://localhost:9090/3")
        
        service.receive(GetGlobalJobStats("testJob"), self)
        expectMsg(JobStats("testJob",Map("FetchResponse" -> 3, "FetchRequest" -> 3)))
        service2.receive(GetGlobalJobStats("testJob"), self)
        expectMsg(JobStats("testJob",Map("FetchResponse" -> 3, "FetchRequest" -> 3)))
        service.stop()
      }
    }

    describe("Terminating a job") {
      it("should work") {
        val service = TestActorRef(TestCrawlService.props, "crawlService8")
        service.receive(RegisterJob(jobConf))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/1"), "testJob"))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/2"), "testJob"))
        receiveN(2).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2")
        service.receive(Broadcast(TerminateJob("testJob")))
        service.receive(new FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob"))
        expectNoMsg()
        service.stop()
      }
    }

  }

}