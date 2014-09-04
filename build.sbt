name := "blikk-crawler"

version := "0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
  "ch.qos.logback" % "logback-classic" % "1.1.2"
)