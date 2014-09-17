// package org.blikk.test

// import akka.routing.{Broadcast, AddRoutee, ActorRefRoutee, Routee, ConsistentHashingGroup}
// import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
// import akka.actor._
// import akka.testkit._
// import org.blikk.crawler._

// class CrawlServiceSpec extends AkkaSingleNodeSpec("CrawlServiceSpec") {

//   describe("CrawlService") {
    
//     describe("Routing a request") {
      
//       it("should work") {
//         val service = system.actorOf(TestCrawlService.props, "crawlService2")
//         service ! TestCrawlService.SetTarget(self)
//         service ! (FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090"), "testJob"))
//         expectMsg("http://localhost:9090")
//         system.stop(service)
//       }

//       it("should work with multiple requests to the same host") {
//         val service = system.actorOf(TestCrawlService.props, "crawlService3")
//         service ! TestCrawlService.SetTarget(self)
//         service ! (FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/1"), "testJob"))
//         service ! (FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/2"), "testJob"))
//         service ! (FetchRequest(WrappedHttpRequest.getUrl("http://localhost:9090/3"), "testJob"))
//         receiveN(3).toSet == Set("http://localhost:9090/1", "http://localhost:9090/2", "http://localhost:9090/3")
//         system.stop(service)
//       }

//     }

//   }

// }