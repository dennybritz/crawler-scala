package org.blikk.crawler

case class RabbitQueueDefinition(name: String, durable: Boolean, exclusive: Boolean=false, 
  autoDelete: Boolean=false, options: Map[String, Object]=Map.empty)
case class RabbitExchangeDefinition(name: String, exchangeType: String, durable: Boolean)

object RabbitData {

  val DataExchange = RabbitExchangeDefinition("blikk-data", "direct", true)
  def queueForApp(appId: String) = 
    RabbitQueueDefinition(s"blikk-${appId}-queue", true, false, false, Map.empty)


}