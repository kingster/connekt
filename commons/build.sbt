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
val akkaVersion = "2.4.3"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-stream" % akkaVersion.concat("-P3") withSources() withJavadoc() exclude("com.typesafe.akka", "akka-actor_2.11"),
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion withSources(),
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion % Test withSources() withJavadoc()
)

libraryDependencies ++= Seq(
  /* Logging Dependencies.Since we want to use log4j2 */
  "com.lmax" % "disruptor" % "3.3.4",
  "org.apache.logging.log4j" % "log4j-api" % "2.5",
  "org.slf4j" % "slf4j-api" % "1.7.13",
  "org.apache.logging.log4j" % "log4j-core" % "2.5",
  "org.apache.logging.log4j" % "log4j-1.2-api" % "2.5", /* For Log4j 1.x API Bridge*/
  "org.apache.logging.log4j" % "log4j-jcl" % "2.5", /* For Apache Commons Logging Bridge */
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.5", /* For SLF4J Bridge */
  /* End of Logging Libraries dependency */
  "commons-pool" % "commons-pool" % "1.6",
  "org.apache.kafka" % "kafka_2.11" % "0.8.2.2" excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule("io.netty")
    ),
  "com.typesafe" % "config" % "1.3.0",
  "commons-beanutils" % "commons-beanutils" % "1.8.0",
  "org.ow2.asm" % "asm" % "4.1",
  "com.esotericsoftware" % "kryo-shaded" % "3.0.3",
  "org.apache.hbase" % "hbase-client" % "1.1.2" excludeAll(
    ExclusionRule("io.netty"),
    ExclusionRule("log4j"),
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "asm")
    ),
  "io.netty" % "netty-all" % "4.1.0.CR6",
  "org.apache.hbase" % "hbase-common" % "1.1.2" excludeAll(
    ExclusionRule("log4j"),
    ExclusionRule("io.netty"),
    ExclusionRule(organization = "org.slf4j")
    ),
  "org.apache.hadoop" % "hadoop-common" % "2.7.2" excludeAll(
    ExclusionRule("org.slf4j"),
    ExclusionRule("io.netty"),
    ExclusionRule("asm"),
    ExclusionRule("log4j"),
    ExclusionRule("org.apache.curator")
    ),
  "com.google.guava" % "guava" % "12.0.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % Test,
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-pool" % "commons-pool" % "1.6",
  "org.springframework" % "spring-jdbc" % "4.2.3.RELEASE",
  "org.apache.commons" % "commons-dbcp2" % "2.1.1",
  "javax.persistence" % "persistence-api" % "1.0.2",
  "mysql" % "mysql-connector-java" % "5.1.37",
  "com.h2database" % "h2" % "1.4.187" % Test,
  "org.apache.velocity" % "velocity" % "1.7",
  "org.codehaus.groovy" % "groovy-all" % "2.4.5",
  "com.roundeights" %% "hasher" % "1.2.0",
  "com.couchbase.client" % "java-client" % "2.1.3",
  "io.reactivex" %% "rxscala" % "0.26.0",
  "org.apache.curator" % "curator-recipes" % "2.10.0" excludeAll(
    ExclusionRule(organization = "com.google.guava"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "io.netty")
    ),
  "com.flipkart.specter" % "specter-client" % "1.4.0-SNAPSHOT",
  "joda-time" % "joda-time" % "2.3",
  "com.flipkart" %% "util-config" % "0.0.1" excludeAll ExclusionRule("com.google.guava", "guava"),
  "com.flipkart" %% "espion" % "1.0.1",
  "com.flipkart" %% "util-http" % "0.0.1-SNAPSHOT",
  "commons-validator" % "commons-validator" % "1.5.0"
)


test in assembly := {}

parallelExecution in Test := false

assemblyMergeStrategy in assembly := AppBuild.mergeStrategy
