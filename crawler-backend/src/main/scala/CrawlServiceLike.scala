package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.stream.actor._
import akka.stream.scaladsl2._
import akka.util.Timeout
import com.rabbitmq.client.{Connection => RabbitMQConnection, Channel => RabbitMQChannel, AMQP}
import org.apache.commons.lang3.SerializationUtils
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
  def frontierProps = Frontier.props(rabbitMQ, self)
  lazy val frontier = {
    val frontierActor = context.actorOf(frontierProps, "frontier")
    frontierActor ! StartFrontier(1.seconds, self)
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
    log.info("Initializing output streams...")
    val input = FlowFrom(ActorPublisher[FetchResponse](responsePublisher))
    val rabbitSubscriber = context.actorOf(RabbitMQSubscriber.props(rabbitMQ), "rabbitSubscriber")
    val rabbitSink = SubscriberSink(ActorSubscriber[FetchResponse](rabbitSubscriber))
    input.withSink(rabbitSink).run()
  }

  def crawlServiceBehavior : Receive = {
    case RouteFetchRequest(fetchReq) => 
      routeFetchRequestGlobally(fetchReq)
    case msg @ FetchRequest(req, appId) =>
      executeFetchRequest(msg)
    case msg: FrontierCommand =>
      frontier ! msg 
  }

  def defaultBehavior : Receive = crawlServiceBehavior

  /* Routes a fetch request using consistent hasing to the right cluster node */
  def routeFetchRequestGlobally(fetchReq: FetchRequest) : Unit = {
    log.debug("routing {}", fetchReq)
    serviceRouter ! ConsistentHashableEnvelope(fetchReq, fetchReq.req.host)
  }

  /* Executes a FetchRequest using Spray's request-level library */
  def executeFetchRequest(fetchReq: FetchRequest) : Unit = {
    log.info("executing {}", fetchReq)
    (IO(Http) ? fetchReq.req.req).mapTo[HttpResponse].map { res =>
      FetchResponse(fetchReq, res)
    } pipeTo responsePublisher
  }


}