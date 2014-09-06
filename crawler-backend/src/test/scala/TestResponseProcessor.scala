package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.testkit._

class TestResponseProcessor(target: ActorRef)(implicit val system: ActorSystem) 
  extends ResponseProcessor {
  
  def name = "TestProcessor"
  def process(res: WrappedHttpResponse, req: WrappedHttpRequest, 
    jobConf: JobConfiguration, context: Map[String, Any]) : Map[String, Any] = {
    target ! "success!"
    context ++ Map(System.nanoTime.toString -> "Yes")
  }
}