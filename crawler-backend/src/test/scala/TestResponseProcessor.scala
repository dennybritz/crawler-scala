package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}

case class TestProcessorOutput(timestamp: Long) extends ProcessorOutput

class TestResponseProcessor(target: ActorRef)(implicit val system: ActorSystem) 
  extends ResponseProcessor {
  def name = "TestProcessor"
  def process(in: ResponseProcessorInput) = {
    target ! in.req.uri.toString
    Map(name -> TestProcessorOutput(System.nanoTime))
  }
}

class RemoteTestResponseProcessor(remoteTarget: String)
  extends ResponseProcessor {
  def name = "RemoteTestProcessor"
  def process(in: ResponseProcessorInput) = {
    // Start a new actor system and send a message to the remote target
    implicit val system = ActorSystem("remoteProcessor")
    val selection = system.actorSelection(remoteTarget)
    selection ! in.req.uri.toString
    Map(name -> TestProcessorOutput(System.nanoTime))
  }
}

class NullProcessor extends ResponseProcessor {
  def name = "NullProcessor"
  def process(in: ResponseProcessorInput) = {
    Map(name -> TestProcessorOutput(System.nanoTime))
  }
}