package org.blikk.test

import akka.routing.Broadcast
import akka.actor.{ActorRef, ActorSystem}
import org.blikk.crawler.channels.{JobOutputChannel, JobChannelInput}
import org.blikk.crawler._

class JobOutputChannelSpec extends AkkaSingleNodeSpec("JobOutputChannelSpec") {

  describe("JobOutputChannel") {

    it("should handle terminate job actions") {
      val input = new JobChannelInput(JobChannelInput.Actions.Stop)
      val joc = new JobOutputChannel(self)
      joc.pipe(input, JobConfiguration.empty("testJob"), Map.empty)
      expectMsg(Broadcast(StopJob("testJob")))
    }

  }

}
