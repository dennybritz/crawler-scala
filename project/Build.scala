import sbt._
import Keys._

object BlikkBuild extends Build {

  lazy val root = Project(id = "blikk", base = file(".")) aggregate(crawlerBackend, 
    crawlerLib, crawlerTest, exampleApp)

  lazy val crawlerBackend = Project(id = "crawler-backend", base = file("./crawler-backend"),
    settings = Project.defaultSettings ++ crawlerSettings) dependsOn(crawlerLib)

  lazy val crawlerLib = Project(id="crawler-lib", base=file("./crawler-lib"), 
    settings = Project.defaultSettings ++ crawlerLibSettings)

  lazy val crawlerTest = Project(id="crawler-tests", base=file("./crawler-test"), 
    settings = Project.defaultSettings ++ crawlerTestSettings) dependsOn(crawlerLib, crawlerBackend)

  lazy val exampleApp = Project(id="example-app", base=file("./example-app"),
    settings = Project.defaultSettings ++ exampleAppSettings) dependsOn(crawlerLib)

  val commonSettings = Seq(
    version := "0.1",
    scalaVersion := "2.11.2",
    resolvers += "Akka Repo Snapshots" at "http://repo.akka.io/snapshots",
    parallelExecution in Test := false,
    fork := true,
    fork in Test := true,
    baseDirectory in run := file(".")
  )

  val crawlerSettings = commonSettings ++ Seq(
    name := "crawler-backend",
    envVars := Map("BLIKK_APP_NAME" -> "crawler-backend"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
      "com.typesafe.akka" %% "akka-contrib" % "2.3.5",
      "io.spray" %% "spray-routing" % "1.3.1"
    ) ++ commonLibraryDependencies
  )

  val crawlerLibSettings = commonSettings ++ Seq(
    name := "crawler-lib",
    libraryDependencies ++= commonLibraryDependencies
  )

  val crawlerTestSettings = commonSettings ++ Seq(
    name := "crawler-test",
    libraryDependencies ++= commonLibraryDependencies ++ Seq(
      "org.scalatest" %% "scalatest" % "2.2.1" % "test",
      "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.3.6",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.5"
    )
  )

  val commonLibraryDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.google.guava" % "guava" % "18.0",
    "com.google.protobuf" % "protobuf-java" % "2.6.0",
    "com.rabbitmq" % "amqp-client" % "3.3.5",
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "0.7",
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.7",
    "com.typesafe.akka" %% "akka-actor" % "2.3.5",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
    "com.typesafe.akka" %% "akka-remote" % "2.3.5",
    "io.spray" %% "spray-can" % "1.3.1",
    "io.spray" %% "spray-client" % "1.3.1",
    "io.spray" %% "spray-http" % "1.3.1",
    "io.spray" %%  "spray-json" % "1.3.0",
    "org.jsoup" % "jsoup" % "1.7.3",
    "org.scalautils" %% "scalautils" % "2.1.5"
  )

  val exampleAppSettings = commonSettings ++ Seq(
    name := "example-app"
  )

}