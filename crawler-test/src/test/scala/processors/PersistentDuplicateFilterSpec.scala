package org.blikk.test

import akka.actor._
import org.scalatest._
import org.blikk.crawler.processors.PersistentDuplicateFilter
import org.blikk.crawler._
import akka.testkit._

class PersistentDuplicateFilterSpec extends AkkaSingleNodeSpec("PersistentDuplicateFilterSpec") {

  var actor : ActorRef = _

  describe("PersistentDuplicateFilter") {

    before {
      actor = system.actorOf(PersistentDuplicateFilter.props("pdf-test"))
      watch(actor)
    }

    after {
      actor ! PersistentDuplicateFilter.DeleteMessages
      actor ! PersistentDuplicateFilter.DeleteSnapshots
      actor ! PersistentDuplicateFilter.Shutdown
      expectMsgClass(classOf[Terminated])
    }

    it("should forward messages not added to the filter") {
      var testProbe = TestProbe()
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item1"), testProbe.ref)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item2"), testProbe.ref)
      testProbe.expectMsg(Some("item1"))
      testProbe.expectMsg(Some("item2"))
    }

    it("should not forward messages not added to the filter") {
      var testProbe = TestProbe()
      actor ! PersistentDuplicateFilter.AddItemCommand("item1")
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item1"), testProbe.ref)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item2"), testProbe.ref)
      testProbe.expectMsg(None)
      testProbe.expectMsg(Some("item2"))
      testProbe.expectNoMsg()
    }

    it("should should work with generic items and a transformation function") {
      actor ! PersistentDuplicateFilter.Shutdown
      expectMsgClass(classOf[Terminated])
      actor = system.actorOf(PersistentDuplicateFilter.props[Int]("pdf-test")(i => (i + 1).toString))
      watch(actor)

      var testProbe = TestProbe()
      actor ! PersistentDuplicateFilter.AddItemCommand(1)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand(1), testProbe.ref)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand(2), testProbe.ref)
      testProbe.expectMsg(None)
      testProbe.expectMsg(Some(2))
      testProbe.expectNoMsg()
    }

    it("recover without snapshots") {
      var testProbe = TestProbe()
      actor ! PersistentDuplicateFilter.AddItemCommand("item1")
      actor ! PersistentDuplicateFilter.Shutdown
      expectMsgClass(classOf[Terminated])
      actor = system.actorOf(PersistentDuplicateFilter.props("pdf-test"))
      watch(actor)

      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item1"), testProbe.ref)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item2"), testProbe.ref)
      testProbe.expectMsg(None)
      testProbe.expectMsg(Some("item2"))
      testProbe.expectNoMsg()
      testProbe.expectNoMsg()
    }

    it("should recover with snapshots") {
      var testProbe = TestProbe()
      actor ! PersistentDuplicateFilter.AddItemCommand("item1")
      actor ! PersistentDuplicateFilter.SaveSnapshot
      // Give the actor some time to save the snapshot
      Thread.sleep(1000)
      actor ! PersistentDuplicateFilter.Shutdown
      expectMsgClass(classOf[Terminated])
      actor = system.actorOf(PersistentDuplicateFilter.props("pdf-test"))
      watch(actor)

      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item1"), testProbe.ref)
      actor.tell(PersistentDuplicateFilter.FilterItemCommand("item2"), testProbe.ref)
      testProbe.expectMsg(None)
      testProbe.expectMsg(Some("item2"))
      testProbe.expectNoMsg()
    }

  }

}