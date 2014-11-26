package org.blikk.crawler.processors

import akka.actor._
import akka.pattern.ask
import akka.persistence._
import akka.stream.actor._
import akka.stream.scaladsl._
import org.blikk.crawler.WrappedHttpRequest
import com.google.common.hash.{BloomFilter, Funnel, Funnels}
import scala.concurrent.duration._

object DuplicateFilter {

  /* Build a duplicate filter based on a string representation of an item */
  def build[A](initialItems : Seq[A] = Nil, 
    expectedInsertions: Int = 1000000, fpp: Double = 0.0001)
  (mapFunc: A => String) : Flow[A,A] = {
    val sdf = new StringDuplicateFilter(expectedInsertions, fpp)
    initialItems.map(mapFunc).foreach(sdf.addItem)
    Flow[A].filter{ item => sdf.filter(mapFunc(item)) }
  }

  /* Builds a duplicate filter based on the URL of the request */
  def buildUrlDuplicateFilter(initialItems : Seq[WrappedHttpRequest] = Nil,
    expectedInsertions: Int = 1000000, fpp: Double = 0.0001) = {
    build[WrappedHttpRequest](initialItems, expectedInsertions, fpp) { req => 
      req.uri.toString
    }
  }

}

/* Filters string duplicates */
class StringDuplicateFilter(val expectedInsertions: Int, val fpp: Double) 
  extends DuplicateFilter[CharSequence] {
  val funnel = Funnels.stringFunnel(java.nio.charset.Charset.defaultCharset)
}


/** 
  * A generic bloom-filter based duplicate eliminator 
  * Sublcasses must provide an appropriate funnel, the expected # of insertions
  * and the false positive probability.
  */
trait DuplicateFilter[A] {

  def expectedInsertions: Int
  def fpp: Double
  def funnel : Funnel[A]
  lazy val bloomFilter : BloomFilter[A] = BloomFilter.create(funnel, expectedInsertions, fpp)

  def filter(item: A) : Boolean = {
    if (bloomFilter.mightContain(item)) {
      false
    } else {
      bloomFilter.put(item)
      true
    }
  }

  /* Manually adds an item to the filter. Useful for initialization. */
  def addItem(item: A) = bloomFilter.put(item)

}


object PersistentDuplicateFilter {

  def props(name: String) = Props(classOf[PersistentDuplicateFilter], name)

  def flow[A](pdf: ActorRef)(implicit system: ActorSystem) : Flow[String, String] = {
    implicit val askTimeout = new akka.util.Timeout(5.seconds)
    Flow[String].mapAsync { item =>
      val result = pdf ? PersistentDuplicateFilter.FilterItemCommand(item, null) 
      pdf ! PersistentDuplicateFilter.AddItemCommand(item)
      result.mapTo[Option[String]]
    }.filter(_.isDefined).map(_.get)
  }

  trait Command
  case class AddItemCommand(item: String) extends Command
  case class FilterItemCommand(item: String, target: ActorRef) extends Command
  case object SaveSnapshot extends Command
  case object DeleteMessages extends Command
  case object DeleteSnapshots extends Command
  case object Shutdown extends Command

  trait Event
  case class ItemAddedEvent(item: String) extends Event

  case class State(bf: BloomFilter[CharSequence]) {
    def update(e: ItemAddedEvent) = bf.put(e.item)
  }

}

class PersistentDuplicateFilter(name: String) extends PersistentActor with ActorLogging {

  import PersistentDuplicateFilter._

  val ExpectedInsertions = 1000000
  val FalsePositiveProb = 0.0001

  override def persistenceId = s"duplicate-filter-${name}"

  def funnel = Funnels.stringFunnel(java.nio.charset.Charset.defaultCharset)
  var state : State = State(
    BloomFilter.create(funnel, ExpectedInsertions, FalsePositiveProb))

  def updateState(e: Event) = e match {
    case event @ ItemAddedEvent(item) => 
      log.debug(event.toString)
      state.update(event)
  }

  val receiveRecover: Receive = {
    case e: Event => updateState(e)
    case SnapshotOffer(metadata, snapshot: State) => 
      log.info("recovering from snapshot: {}", metadata)
      state = snapshot
    case RecoveryCompleted => log.info("recovery completed")
  }

  val receiveCommand: Receive = {
    //case x => log.info(x.toString)
    case AddItemCommand(item) => persist(ItemAddedEvent(item))(updateState)
    case FilterItemCommand(item, _) => 
      sender ! (if (state.bf.mightContain(item)) None else Some(item))
    case SaveSnapshot => 
      log.info("saving snapshot")
      saveSnapshot(state)
    case DeleteMessages => 
      log.info("deleting journaled messages up to lastSequenceNr={}", lastSequenceNr)
      deleteMessages(lastSequenceNr, true)
    case DeleteSnapshots =>
      log.info("deleting snapshots up to snapshotSequenceNr={}", snapshotSequenceNr)
      deleteSnapshots(SnapshotSelectionCriteria.latest)
    case SaveSnapshotSuccess(metadata) => log.info("saved snapshot")
    case SaveSnapshotFailure(metadata, reason) => log.error(reason, "snapshot could not be saved")
    case Shutdown => context.stop(self) 
  }


}


