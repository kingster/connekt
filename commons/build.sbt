import AppBuild._
import sbt.ExclusionRule

name := "commons"

version := "1.0-SNAPSHOT"

envKey := {
  System.getProperty("env.key") match {
    case null => "default"
    case defined: String => defined
  }
}


/** all akka only **/
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.2-RC2" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-stream" % "2.4.2-RC2" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-http-core" % "2.4.2-RC2" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2-RC2" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % "2.4.2-RC2" % Test withSources() withJavadoc()
)

libraryDependencies ++= Seq(
  "commons-pool" % "commons-pool" % "1.6",
  "org.apache.kafka" % "kafka_2.11" % "0.8.2.2" excludeAll
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
  "com.typesafe" % "config" % "1.3.0",
  "commons-beanutils" % "commons-beanutils" % "1.8.0",
  "org.ow2.asm" % "asm" % "4.1",
  "com.flipkart.kloud.config" % "client-java" % "1.0.8",
  "com.esotericsoftware" % "kryo-shaded" % "3.0.3",
  "org.apache.hbase" % "hbase" % "0.94.15-cdh4.7.0" excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "javax.servlet"),
    ExclusionRule(organization = "javax.servlet.jsp"),
    ExclusionRule(organization = "tomcat"),
    ExclusionRule(organization = "org.eclipse.jetty.orbit"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.jruby", name = "jruby-complete"),
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "org.mortbay.jetty", name = "servlet-api-2.5"),
    ExclusionRule(organization = "org.mockito"),
    ExclusionRule(organization = "junit"),
    ExclusionRule(organization = "asm"),
    ExclusionRule(organization = "javax.xml.stream", name = "stax-api"),
    ExclusionRule(organization = "com.google.guava", name = "guava"),
    ExclusionRule(organization = "jline", name = "jline")
    ),
  "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.7.0" excludeAll(
    ExclusionRule(organization = "javax.servlet"),
    ExclusionRule(organization = "tomcat"),
    ExclusionRule(organization = "javax.servlet.jsp"),
    ExclusionRule(organization = "org.eclipse.jetty.orbit"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.mockito"),
    ExclusionRule(organization = "junit"),
    ExclusionRule(organization = "asm"),
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "org.jruby", name = "jruby-complete"),
    ExclusionRule(organization = "org.mortbay.jetty", name = "servlet-api-2.5"),
    ExclusionRule(organization = "javax.xml.stream", name = "stax-api"),
    ExclusionRule(organization = "com.google.guava", name = "guava"),
    ExclusionRule(organization = "jline", name = "jline"),
    ExclusionRule(organization = "commons-beanutils")
    ),
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % Test,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-core" % "1.1.3",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.3",
  "com.lmax" % "disruptor" % "3.3.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-pool" % "commons-pool" % "1.6",
  "org.springframework" % "spring-jdbc" % "4.2.3.RELEASE",
  "org.apache.commons" % "commons-dbcp2" % "2.1.1",
  "javax.persistence" % "persistence-api" % "1.0.2",
  "mysql" % "mysql-connector-java" % "5.1.37",
  "com.h2database" % "h2" % "1.4.187" % Test,
  "com.google.guava" % "guava" % "19.0",
  "org.apache.velocity" % "velocity" % "1.7",
  "org.codehaus.groovy" % "groovy-all" % "2.4.5",
  "com.roundeights" %% "hasher" % "1.2.0",
  "com.couchbase.client" % "java-client" % "2.1.3",
  "com.google.guava" % "guava" % "18.0",
  "io.reactivex" %% "rxscala" % "0.26.0",
  "org.apache.curator" % "curator-recipes" % "2.9.1",
  "com.flipkart.specter" % "specter-client" % "1.1.4",
  "joda-time" % "joda-time" % "2.3"
)


test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy