package org.blikk.test

import akka.actor.{ActorRef, ActorSystem}
import org.blikk.crawler.channels.{JobOutputChannel, JobChannelInput}
import org.blikk.crawler._

// Sends the message to the target instead of a predefined path for testing
class TestJOC(target: ActorRef)(implicit system: ActorSystem) extends JobOutputChannel {
  override def serviceActor = system.actorSelection(target.path) 
}

class JobOutputChannelSpec extends AkkaSingleNodeSpec("JobOutputChannelSpec") {

  describe("JobOutputChannel") {

    it("should handle terminate job actions") {
      val input = new JobChannelInput(JobChannelInput.Actions.Terminate)
      val joc = new TestJOC(self)
      joc.pipe(input, JobConfiguration.empty("testJob"), Map.empty)
      expectMsg(TerminateJob("testJob"))
    }

  }

}
