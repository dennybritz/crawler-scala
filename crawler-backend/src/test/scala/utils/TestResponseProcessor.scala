package org.blikk.test

import org.blikk.crawler._
import akka.actor._
import akka.testkit._
import akka.event.Logging
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
    implicit val system = ActorSystem("remoteProcessor",
      ConfigFactory.parseString("""
        akka.remote.netty.tcp.port = 0
        akka.actor.provider = akka.remote.RemoteActorRefProvider"""
      ).withFallback(ConfigFactory.load()))
    
    val log = Logging.getLogger(system, this)
    val selection = system.actorSelection(remoteTarget)
    log.info("Sending {} tp {}", in.req.uri.toString, selection)
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