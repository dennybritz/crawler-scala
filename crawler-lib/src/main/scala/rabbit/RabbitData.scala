package org.blikk.crawler

/* RabbotMQ queue properties */
case class RabbitQueueDefinition(
  name: String, 
  durable: Boolean, 
  exclusive: Boolean = false, 
  autoDelete: Boolean = false, 
  options: Map[String, Object] = Map.empty)

/* RabbitMQ exchange properties */
case class RabbitExchangeDefinition(
  name: String, 
  exchangeType: String, 
  durable: Boolean)

/** 
  * RabbitMQ data definitions shared by several classes 
  * Note: server-side definition are not included
  */
object RabbitData {

  // Used to exchange messages between crawl platform and crawl apps
  val DataExchange = RabbitExchangeDefinition("blikk-data", "direct", true)

  def queueForApp(appId: String) = 
    RabbitQueueDefinition(s"blikk-${appId}-queue", true, false, false, Map.empty)


}