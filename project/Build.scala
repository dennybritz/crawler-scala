import sbt._
import Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

object BlikkBuild extends Build {

  lazy val root = Project(id = "blikk", base = file(".")) aggregate(crawlerBackend, crawlerLib)

  lazy val crawlerBackend = Project(id = "blikk-crawler-backend", base = file("./crawler-backend"),
    settings = Project.defaultSettings ++ crawlerSettings ++ multiJvmSettings) dependsOn(crawlerLib) configs (MultiJvm) 

  lazy val crawlerLib = Project(id="blikk-crawler-lib", base=file("./crawler-lib"), 
    settings = Project.defaultSettings ++ crawlerLibSettings)

  val crawlerSettings = Seq(
    name := "blikk-crawler-backend",
    version := "0.1",
    scalaVersion := "2.11.2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.5",
      "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.5",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
      "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.3.6",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "io.spray" %% "spray-http" % "1.3.1",
      "io.spray" %% "spray-can" % "1.3.1",
      "io.spray" %% "spray-routing" % "1.3.1",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test",
      "org.scalautils" %% "scalautils" % "2.1.5",
      "com.rabbitmq" % "amqp-client" % "3.3.5",
      "org.jsoup" % "jsoup" % "1.7.3",
      "net.debasishg" %% "redisclient" % "2.13",
      "com.esotericsoftware.kryo" % "kryo" % "2.24.0"
    ),
    parallelExecution in Test := false
  )

  val crawlerLibSettings = Seq(
    name := "blikk-crawler-lib",
    version := "0.1",
    scalaVersion := "2.11.2",
     libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.5",
      "io.spray" %% "spray-http" % "1.3.1",
      "io.spray" %% "spray-can" % "1.3.1",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test",
      "org.scalautils" %% "scalautils" % "2.1.5",
      "org.jsoup" % "jsoup" % "1.7.3",
      "com.esotericsoftware.kryo" % "kryo" % "2.24.0"
    ),
    parallelExecution in Test := false
  )

  val multiJvmSettings = Seq(
    // make sure that MultiJvm test are compiled by the default test compilation
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    parallelExecution in Test := false,
    fork in run := true
    // make sure that MultiJvm tests are executed by the default test target, 
    // and combine the results from ordinary test and multi-jvm tests
    // executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
    //   case (testResults, multiNodeResults)  =>
    //     val overall =
    //       if (testResults.overall.id < multiNodeResults.overall.id)
    //         multiNodeResults.overall
    //       else
    //         testResults.overall
    //     Tests.Output(overall,
    //       testResults.events ++ multiNodeResults.events,
    //       testResults.summaries ++ multiNodeResults.summaries)
    // }
  ) ++ SbtMultiJvm.multiJvmSettings

}