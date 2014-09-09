package org.blikk.test

import akka.actor._
import akka.testkit._
import org.blikk.crawler._

class JobStatsCollectorSpec extends AkkaSingleNodeSpec("JobStatsCollectorSpec") with LocalRedis {

  describe("JobStatsCollector") {


    it("should collect statistics for multiple events correctly") {
      val actor = TestActorRef(JobStatsCollector.props(localRedis))
      actor ! ClearJobEventCounts("testJob1")
      actor ! ClearJobEventCounts("testJob2")
      actor ! JobEvent("testJob1", "request")
      actor ! JobEvent("testJob1", "request")
      actor ! JobEvent("testJob1", "request")
      actor ! JobEvent("testJob1", "response")
      actor ! JobEvent("testJob1", "response")
      actor ! GetJobEventCounts("testJob1")
      expectMsg(JobStats("testJob1", Map("testJob1:request" -> 3, "testJob1:response" -> 2)))
      actor ! JobEvent("testJob1", "request")
      actor ! JobEvent("testJob1", "response")
      actor ! GetJobEventCounts("testJob1")
      expectMsg(JobStats("testJob1", Map("testJob1:request" -> 4, "testJob1:response" -> 3)))
      actor.stop()
    }

    it("should collect statistics from the event stream") {
      val actor = TestActorRef(JobStatsCollector.props(localRedis))
      actor ! ClearJobEventCounts("testJob1")
      actor ! ClearJobEventCounts("testJob2")
      system.eventStream.publish(JobEvent("testJob1", "request"))
      system.eventStream.publish(JobEvent("testJob1", "request"))
      actor ! GetJobEventCounts("testJob1")
      expectMsg(JobStats("testJob1", Map("testJob1:request" -> 2)))
      actor.stop()
    }

  }

}