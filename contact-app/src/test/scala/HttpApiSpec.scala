// package org.blikk.contactapp

// import org.scalatest._
// import akka.testkit._
// import akka.actor._
// import akka.io._
// import spray.can._
// import spray.http._
// import spray.testkit.Specs2RouteTest
// import spray.routing.HttpService

// class HttpApiSpec extends TestKit(ActorSystem("HttpApiSpec")) 
//   with FunSpecLike with BeforeAndAfterAll with ImplicitSender {

//    override def beforeAll() {
//     // Start the HTTP API
//     val listener = system.actorOf(HttpApi.props(self), "api")
//     IO(Http) ! Http.Bind(listener, "localhost", 9090)
//   }

//   describe("HTTP API") {

//     it("should accept JSON job requests"){

//     }
//   }

// }