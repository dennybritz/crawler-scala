package org.blikk.crawler

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.cluster.routing._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.util.Timeout
import com.rabbitmq.client.{Connection => RabbitMQConnection, Channel => RabbitMQChannel, AMQP}
import org.apache.commons.lang3.SerializationUtils
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import spray.can.Http
import spray.http._

trait CrawlServiceLike { this: Actor with ActorLogging =>

  implicit val askTimeout = Timeout(5.seconds)
  import context.system
  import context.dispatcher

  /* A thread-local RabbitMQ channel */
  implicit def rabbitMQ: RabbitMQConnection
  lazy val rabbitMQChannel = new ThreadLocal[RabbitMQChannel] {
    override def initialValue = { 
      val channel = rabbitMQ.createChannel()
      channel.exchangeDeclare("blikk-data", "direct", false)
      channel
    }
  }

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

  def crawlServiceBehavior : Receive = {
    case RouteFetchRequest(fetchReq) => 
      routeFetchRequestGlobally(fetchReq)
    case msg @ FetchRequest(req, jobId) =>
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
    val futureResponse = (IO(Http) ? fetchReq.req.req).mapTo[HttpResponse]
    futureResponse.onComplete { 
      case Success(response : HttpResponse) =>
        writeFetchResponse(new WrappedHttpResponse(response), fetchReq)
      case Failure(err) => 
        log.error("Error fetching {}", fetchReq)
        log.error(err.toString)
    }
  }

  /* Writes a CrawlItem to the data store (RabbitMQ) */
  def writeFetchResponse(response: WrappedHttpResponse, fetchReq: FetchRequest) : Unit = {
    val item = CrawlItem(fetchReq.req, response)
    val serializedItem = SerializationUtils.serialize(item)
    val channel = rabbitMQChannel.get()
    log.info("writing numBytes={} to RabbitMQ", serializedItem.size)
    channel.basicPublish("blikk-data", fetchReq.jobId, null, serializedItem)
  }

}