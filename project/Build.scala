import sbt._
import Keys._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

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
    version := "0.1.0",
    scalaVersion := "2.11.4",
    resolvers += "Akka Repo Snapshots" at "http://repo.akka.io/snapshots",
    parallelExecution in Test := false,
    fork := true,
    fork in Test := true,
    baseDirectory in run := file(".")
  )

  val AkkaBaseVersion = "2.3.7"

  val commonLibraryDependencies = Seq(
    "commons-codec" % "commons-codec" % "1.10",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.google.guava" % "guava" % "18.0",
    "com.google.protobuf" % "protobuf-java" % "2.6.0",
    "com.rabbitmq" % "amqp-client" % "3.3.5",
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-M1",
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-M1",
    "com.typesafe.akka" %% "akka-actor" % AkkaBaseVersion,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaBaseVersion,
    "com.typesafe.akka" %% "akka-remote" % AkkaBaseVersion,
    "com.typesafe.akka" %% "akka-persistence-experimental" % AkkaBaseVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaBaseVersion,
    "io.spray" %% "spray-can" % "1.3.1",
    "io.spray" %% "spray-client" % "1.3.1",
    "io.spray" %% "spray-http" % "1.3.1",
    "io.spray" %%  "spray-json" % "1.3.0",
    "org.jsoup" % "jsoup" % "1.7.3",
    "org.scalautils" %% "scalautils" % "2.1.5",
    "org.xerial.snappy" % "snappy-java" % "1.1.1.6",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.3.2",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )

  val crawlerSettings = commonSettings ++ Seq(
    name := "crawler-backend",
    envVars := Map("BLIKK_APP_NAME" -> "crawler-backend"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % AkkaBaseVersion,
      "com.typesafe.akka" %% "akka-contrib" % AkkaBaseVersion,
      "io.spray" %% "spray-routing" % "1.3.1"
    ) ++ commonLibraryDependencies
  ) ++ packageArchetype.java_application

  val crawlerLibSettings = commonSettings ++ Seq(
    name := "crawler-lib",
    libraryDependencies ++= commonLibraryDependencies
  )

  val crawlerTestSettings = commonSettings ++ Seq(
    name := "crawler-test",
    libraryDependencies ++= commonLibraryDependencies
  )

  val exampleAppSettings = commonSettings ++ Seq(
    name := "example-app"
  ) ++ packageArchetype.java_application

}