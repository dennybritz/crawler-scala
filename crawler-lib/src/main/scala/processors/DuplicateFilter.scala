package org.blikk.crawler.processors

import akka.actor._
import akka.pattern.ask
import akka.persistence._
import akka.stream.actor._
import akka.stream.scaladsl._
import org.blikk.crawler.WrappedHttpRequest
import com.google.common.hash.{BloomFilter, Funnel, Funnels}
import scala.concurrent.duration._
import scala.reflect.runtime.universe._

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

  def props(name: String) = Props(classOf[PersistentDuplicateFilter[String]], name, (x: String) => x)

  def props[A](name: String)(f: A => String) = Props(classOf[PersistentDuplicateFilter[A]], name, f)

  def flow[A](pdf: ActorRef)(implicit system: ActorSystem) : Flow[A, A] = {
    implicit val askTimeout = new akka.util.Timeout(5.seconds)
    Flow[A].mapAsync { item =>
      val result = pdf ? PersistentDuplicateFilter.FilterItemCommand(item) 
      pdf ! PersistentDuplicateFilter.AddItemCommand(item)
      result.mapTo[Option[A]]
    }.filter(_.isDefined).map(_.get)
  }

  trait Command
  case class AddItemCommand[A](item: A) extends Command
  case class FilterItemCommand[A](item: A) extends Command
  case object SaveSnapshot extends Command
  case object DeleteMessages extends Command
  case object DeleteSnapshots extends Command
  case object Shutdown extends Command

  trait Event
  case class ItemAddedEvent[A](item: A) extends Event

  case class State(bf: BloomFilter[CharSequence])

}

class PersistentDuplicateFilter[A](name: String)(t: A => String)
  extends PersistentActor with ActorLogging {

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
      state.bf.put(t(item.asInstanceOf[A]))
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
    case FilterItemCommand(item) => 
      sender ! (if (state.bf.mightContain(t(item.asInstanceOf[A]))) None else Some(item))
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


