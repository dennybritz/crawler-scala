// package org.blikk.test

// import org.blikk.crawler._
// import org.blikk.crawler.processors._
// import scala.concurrent.duration._

// class MultiJobIntegrationSpec extends IntegrationSuite("MultiJobIntegrationSpec") {

//   describe("A distributed crawler") {
    
//     it("should work with multiple jobs at the same time"){
      
//       val processors1 = List(new LinkExtractor("link-extractor"),
//         new TestResponseProcessor(probes(1).ref)(systems(1)))
//       val jobConf1 = new JobConfiguration("MultiJobIntegrationSpecJob1",
//         List(WrappedHttpRequest.getUrl("http://localhost:9090/links/1")), processors1)

//       val processors2 = List(new LinkExtractor("link-extractor"),
//         new TestResponseProcessor(probes(2).ref)(systems(2)))
//       val jobConf2 = new JobConfiguration("MultiJobIntegrationSpecJob2",
//         List(WrappedHttpRequest.getUrl("http://localhost:9090/links/500"), 
//           WrappedHttpRequest.getUrl("http://localhost:9090/links/600")), processors2)

//       services(0) ! RunJob(jobConf1)
//       services(0) ! RunJob(jobConf2)

//       assert(probes(1).receiveN(3, 5.seconds).toSet === Set("http://localhost:9090/links/1", "http://localhost:9090/links/2", 
//         "http://localhost:9090/links/3"))
//       assert(probes(2).receiveN(2, 5.seconds).toSet === Set("http://localhost:9090/links/500",
//         "http://localhost:9090/links/600"))

//       probes.foreach(_.expectNoMsg())
//     }
//   }

// }