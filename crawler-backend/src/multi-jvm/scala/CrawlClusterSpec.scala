package org.blikk.test

import com.typesafe.config.ConfigFactory
import akka.remote.testkit.{MultiNodeSpecCallbacks, MultiNodeConfig, MultiNodeSpec}
import org.scalatest.{FunSpecLike, BeforeAndAfter, BeforeAndAfterAll}
import akka.testkit.ImplicitSender

trait CrawlClusterSpecLike extends MultiNodeSpecCallbacks
  with FunSpecLike with BeforeAndAfterAll { 
 
  override def beforeAll() = {
    multiNodeSpecBeforeAll()
  }
  
  override def afterAll() = {
    multiNodeSpecAfterAll()
  }

}

object CrawlClusterConfig extends MultiNodeConfig {
  
  val node1 = role("node1")
  val node2 = role("node2")
  val node3 = role("node3")

  def nodes = Seq(node1, node2, node3)

  commonConfig(ConfigFactory.load("application.multiJvm"))
}


class CrawlClusterSpec extends MultiNodeSpec(CrawlClusterConfig)
  with CrawlClusterSpecLike with ImplicitSender {
  
  import CrawlClusterConfig._

  def startHttpServer(port: Int = 9090) : Unit = {
    runOn(node1) {
      TestHttpServer.start("localhost", port)
    }
    testConductor.enter("http-server-started")
  }

  import CrawlClusterConfig._

  def initialParticipants = roles.size

}