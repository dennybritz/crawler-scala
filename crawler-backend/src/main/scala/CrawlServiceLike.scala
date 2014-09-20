package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.util.Timeout
import com.rabbitmq.client.{Connection => RabbitMQConnection, Channel => RabbitMQChannel, AMQP}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import spray.can.Http
import spray.http._

trait CrawlServiceLike { 
  this: Actor with ActorLogging with ImplicitFlowMaterializer =>

  implicit val askTimeout = Timeout(5.seconds)
  import context.system
  import context.dispatcher
  
  implicit def rabbitMQ: RabbitMQConnection

  /* The frontier */
  def frontierProps = Frontier.props(rabbitMQ, serviceRouter)
  lazy val frontier = {
    val frontierActor = context.actorOf(frontierProps, "frontier")
    context.watch(frontierActor)
    frontierActor
  }
  
  /* Routes request globally across the cluster */
  def serviceRouter : ActorRef 

  /* Asks peers using scatter-gather */
  def peerScatterGatherRouter : ActorRef

  /* Converts fetched items into a stream */
  lazy val responsePublisher = {
    context.actorOf(Props[ResponsePublisher], "responsePublisher")
  }

  /** 
    * IMPORTANT: Call ths method in preStart() when subclasses.
    * We initialize the response data stream that writes out the data
    */
  def initializeSinks() {
    // Might as well initialize the frontier here
    frontier
    log.info(flowMaterializerSettings.toString)
    log.info("Initializing output streams...")
    val input = FlowFrom(ActorPublisher[FetchResponse](responsePublisher))
    val rabbitSubscriber = context.actorOf(RabbitMQSubscriber.props(rabbitMQ), "rabbitSubscriber")
    val rabbitSink = SubscriberSink(ActorSubscriber[FetchResponse](rabbitSubscriber))
    input.withSink(rabbitSink).run()
  }

  def crawlServiceBehavior : Receive = {
    case msg @ FetchRequest(req, appId) =>
      executeFetchRequest(msg)
    case msg: FrontierCommand =>
      frontier ! msg 
  }

  def defaultBehavior : Receive = crawlServiceBehavior

  /* Executes a FetchRequest using Spray's request-level library */
  def executeFetchRequest(fetchReq: FetchRequest) : Unit = {
    log.info("executing {}", fetchReq)
    (IO(Http) ? fetchReq.req.req).mapTo[HttpResponse].map { res =>
      FetchResponse(fetchReq, res)
    } pipeTo responsePublisher
  }


}