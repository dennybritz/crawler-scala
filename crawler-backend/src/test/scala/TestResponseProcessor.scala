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

class RemoteTestResponseProcessor(remoteTarget: String)
  extends ResponseProcessor {
  
  def name = "RemoteTestProcessor"
  def process(res: WrappedHttpResponse, req: WrappedHttpRequest, 
    jobConf: JobConfiguration, context: Map[String, Any]) : Map[String, Any] = {
    // Start a new actor system and send a message to the remote target
    implicit val system = ActorSystem("remoteProcessor")
    val selection = system.actorSelection(remoteTarget)
    selection ! "success!"
    context ++ Map(System.nanoTime.toString -> "Yes")
  }
}


class NullProcessor extends ResponseProcessor {
  def name = "NullProcessor"
  def process(res: WrappedHttpResponse, 
    req: WrappedHttpRequest, 
    jobConf: JobConfiguration, 
    context: Map[String, Any]) : Map[String, Any] = {
    context
  }
}