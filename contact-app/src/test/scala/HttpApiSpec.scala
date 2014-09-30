package org.blikk.contactapp

import org.scalatest._
import akka.testkit._
import akka.actor._
import akka.io._
import spray.can._
import spray.http._

class HttpApiSpec extends TestKit(ActorSystem("HttpApiSpec")) 
  with FunSpecLike with BeforeAndAfterAll with ImplicitSender {

   override def beforeAll() {
    // Start the HTTP
    val listener = system.actorOf(HttpApi.props(self), "api")
    IO(Http) ! Http.Bind(listener, "localhost", 9090)
  }

  describe("HTTP API") {

    it("should accept JSON job requests")(pending)

  }

}