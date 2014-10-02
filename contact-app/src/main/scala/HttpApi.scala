// package org.blikk.contactapp

// import akka.actor._
// import akka.pattern.ask
// import akka.util.Timeout
// import spray.httpx.marshalling._
// import spray.httpx.unmarshalling._
// import spray.httpx.SprayJsonSupport._
// import spray.json._
// import spray.routing._
// import scala.concurrent.duration._
// import scala.util.{Try, Success, Failure}

// import DefaultJsonProtocol._

// object HttpApi {
//   case class Job(url: String)
//   implicit val jobFormat = jsonFormat1(Job)
//   def props(jobManager: ActorRef) = Props(classOf[HttpApi], jobManager)
// }

// class HttpApi(jobManager: ActorRef) extends HttpServiceActor with ActorLogging {

//   import HttpApi._
//   import context.dispatcher

//   implicit val askTimeout = Timeout(5.seconds) 

//   def receive = runRoute {
//     post {
//       path("jobs") {
//         entity(as[Job]) { j =>
//           val appIdF = (jobManager ? StartJob(j.url, 1.minute)).mapTo[String]
//           complete(appIdF)
//         }
//       }
//     } 
//   }

// }
