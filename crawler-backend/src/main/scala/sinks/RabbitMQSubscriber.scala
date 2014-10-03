// package org.blikk.crawler

// import akka.actor._
// import akka.stream.actor._
// import com.rabbitmq.client.{Connection => RabbitConnection, Channel => RabbitChannel, AMQP}

// object RabbitMQSubscriber {
//   def props(channel: RabbitChannel) 
//     = Props(classOf[RabbitMQSubscriber], channel)
// }

// /**
//   * Subscribes to the stream of request-response objects and publishes them into RabbitMQ
//   */
// class RabbitMQSubscriber(rabbitMQChannel: RabbitChannel) extends Actor with ActorLogging 
//   with ActorSubscriber {

//   import ActorSubscriberMessage._

//   // Is 100 a good value? I have no idea.
//   def requestStrategy = WatermarkRequestStrategy(100)

//   override def preStart(){
//     log.info("starting")
//   }

//   override def postStop(){
//     rabbitMQChannel.close()
//   }

//   def receive = {
//     case next @ OnNext(item: FetchResponse) => writeFetchResponse(item)
//     case complete @ OnComplete =>
//       log.info("stream is finished. shutting down.")
//       context.stop(self)
//     case err @ OnError(cause) =>
//       log.error("stream error, shutting down: {}", cause.toString)
//       context.stop(self)
//   }

//   /* Writes a CrawlItem to RabbitMQ */
//   def writeFetchResponse(fetchRes: FetchResponse) : Unit = {
//     //log.debug(fetchRes.res.toString)
//     SerializationUtils.serialize(fetchRes.res)
//     val item = CrawlItem(fetchRes.fetchReq.req, fetchRes.res)
//     val serializedItem = SerializationUtils.serialize(item)
//     log.debug("writing result for url=\"{}\" appId=\"{}\" numBytes={} to RabbitMQ", 
//       fetchRes.fetchReq.req.uri, fetchRes.fetchReq.appId, serializedItem.size)
//     rabbitMQChannel.basicPublish(
//       RabbitData.DataExchange.name, 
//       fetchRes.fetchReq.appId, 
//       null, 
//       serializedItem)
//   }


// }