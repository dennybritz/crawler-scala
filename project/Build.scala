import sbt._
import Keys._
import com.typesafe.sbt.SbtStartScript

object BlikkBuild extends Build {

  lazy val root = Project(id = "blikk", base = file(".")) aggregate(crawlerBackend, crawlerLib, crawlerTest)

  lazy val crawlerBackend = Project(id = "blikk-crawler-backend", base = file("./crawler-backend"),
    settings = Project.defaultSettings ++ crawlerSettings ++ 
    SbtStartScript.startScriptForClassesSettings) dependsOn(crawlerLib)

  lazy val crawlerLib = Project(id="blikk-crawler-lib", base=file("./crawler-lib"), 
    settings = Project.defaultSettings ++ crawlerLibSettings)

  lazy val crawlerTest = Project(id="blikk-crawler-tests", base=file("./crawler-test"), 
    settings = Project.defaultSettings ++ crawlerTestSettings) dependsOn(crawlerLib, crawlerBackend)

  val commonSettings = Seq(
    version := "0.1",
    scalaVersion := "2.11.2",
    resolvers += "Akka Repo Snapshots" at "http://repo.akka.io/snapshots",
    parallelExecution in Test := false,
    fork in Test := true,
    baseDirectory in run := file(".")
  )

  val crawlerSettings = commonSettings ++ Seq(
    name := "blikk-crawler-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
      "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
      "io.spray" %% "spray-routing" % "1.3.1"
    ) ++ commonLibraryDependencies
  )

  val crawlerLibSettings = commonSettings ++ Seq(
    name := "blikk-crawler-lib",
    libraryDependencies ++= commonLibraryDependencies
  )

  val crawlerTestSettings = commonSettings ++ Seq(
    name := "blikk-crawler-test",
    libraryDependencies ++= commonLibraryDependencies ++ Seq(
      "org.scalatest" %% "scalatest" % "2.2.1" % "test",
      "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.3.6",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.5"
    )
  )

  val commonLibraryDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.google.guava" % "guava" % "18.0",
    "com.rabbitmq" % "amqp-client" % "3.3.5",
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "0.7",
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.7",
    "com.typesafe.akka" %% "akka-actor" % "2.3.5",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
    "io.spray" %% "spray-can" % "1.3.1",
    "io.spray" %% "spray-http" % "1.3.1",
    "org.apache.commons" % "commons-lang3" % "3.3.2",
    "org.jsoup" % "jsoup" % "1.7.3",
    "org.scalautils" %% "scalautils" % "2.1.5"
  )


}