name := "commons"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "commons-pool" % "commons-pool" % "1.6",
  "org.apache.kafka" % "kafka_2.11" % "0.8.2.2" excludeAll
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
  "com.typesafe" % "config" % "1.3.0",
  "com.flipkart.kloud.config" % "client-java" % "1.0.8",
  "org.apache.hbase" % "hbase" % "0.94.15-cdh4.7.0" excludeAll
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
  "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.7.0" excludeAll
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-core" % "1.1.3",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.3",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.0",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M1",
  "com.lmax" % "disruptor" % "3.3.2"
)