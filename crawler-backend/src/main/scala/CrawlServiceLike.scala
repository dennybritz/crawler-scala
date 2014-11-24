package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.stream.OverflowStrategy
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraphImplicits._
import akka.util.Timeout
import com.rabbitmq.client.{Connection => RabbitMQConnection, Channel => RabbitMQChannel, AMQP}
import org.blikk.crawler.processors.RabbitMQSink
import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.util.{Try, Success, Failure}
import spray.can.Http
import spray.http._
import spray.httpx.encoding._
import spray.httpx.ResponseTransformation._
import org.xerial.snappy.Snappy

trait CrawlServiceLike { 
  this: Actor with ActorLogging with ImplicitFlowMaterializer =>
  
  import context.system
  import context.dispatcher

  implicit val askTimeout = Timeout(Config.requestTimeout)
  
  // Delay for requests to the same domain
  val defaultDelay = 500 
  // We can keep this many messages per domain in a buffer 
  val perDomainBuffer = 5000
  // We send out this many requests at once. Useful for HTTP pipelining
  val requestBlockSize = 1

  /* The frontier */
  def frontierProps = Frontier.props(self)
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
    
    // Initialize RabbitMQ data
    Try(RabbitData.createChannel()) match {
      case Success(rabbitChannel) => 
        Resource.using(rabbitChannel) { implicit channel => RabbitData.declareAll() }
      case Failure(err) => 
        log.error(err, "Could not connect to RabbitMQ")
        context.system.shutdown()
        return System.exit(1)
    }

    // Might as well initialize the frontier here
    frontier
    log.info("Initializing output streams...")
    val input = Source(ActorPublisher[FetchResponse](responsePublisher))
    val rabbitSink = RabbitMQSink.build[FetchResponse](RabbitData.createChannel(), 
      RabbitData.DataExchange) { fetchRes =>
      val item = CrawlItem(fetchRes.fetchReq.req, fetchRes.res, fetchRes.fetchReq.appId)
      val serializedItem = SerializationUtils.toProto(item).toByteArray
      val compressedItem =  Snappy.compress(serializedItem)
      List((compressedItem, fetchRes.fetchReq.appId))
    }

    val transformer = new ESRabbitRiverTransformer()
    val rabbitElasticSearchSink = RabbitMQSink.build[FetchResponse](RabbitData.createChannel(), 
      Config.ElasticSearchDataExchange) { fetchRes =>
      val jsonData = transformer.transform(fetchRes)
      val routingKey = fetchRes.fetchReq.req.topPrivateDomain.getOrElse("")
      jsonData.map ( x => (x.getBytes, routingKey)).toList
      
    }

    val responseBroadcast = Broadcast[FetchResponse]

    FlowGraph { implicit b =>
      input ~> responseBroadcast
      responseBroadcast ~> rabbitSink
      responseBroadcast ~> rabbitElasticSearchSink
    }.run()

    log.info("crawler ready :)")
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
    log.info("fetching url=\"{}\"", fetchReq.req.uri.toString)
    val respFuture = (IO(Http) ? WrappedHttpRequest.toSpray(fetchReq.req)).mapTo[HttpResponse]
    (respFuture.map(decode(NoEncoding) ~> decode(Gzip) ~> decode(Deflate))).map { res =>
      FetchResponse(fetchReq, res)
    } pipeTo responsePublisher
  }


}