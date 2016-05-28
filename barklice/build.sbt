import sbt.ExclusionRule
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._


libraryDependencies ++= Seq(
  "chronosQ" % "ha-worker" % "1.1-SNAPSHOT"  excludeAll(
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "jline", name = "jline"),
    ExclusionRule(organization = "com.codahale.metrics"),
    ExclusionRule(organization = "org.slf4j", name = "slf4j-simple"),
    ExclusionRule(organization = "org.mockito", name = "mockito-all"),
    ExclusionRule(organization = "org.hamcrest", name = "hamcrest-core"),
    ExclusionRule(organization = "asm"),
    ExclusionRule(organization= "com.google.code.findbugs",name = "annotations")
    ),
  "chronosQ" % "chronosQ-core" % "1.1-SNAPSHOT" excludeAll(
    ExclusionRule(organization = "jline", name = "jline"),
    ExclusionRule(organization = "com.codahale.metrics"),
    ExclusionRule(organization = "org.mockito", name = "mockito-all"),
    ExclusionRule(organization = "org.hamcrest", name = "hamcrest-core")
    ),
  "chronosQ" % "chronosQ-reservation-impl-kafka" % "1.1-SNAPSHOT" excludeAll(
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.apache.kafka"),
    ExclusionRule(organization = "jline", name = "jline"),
    ExclusionRule(organization = "com.codahale.metrics"),
    ExclusionRule(organization = "org.slf4j", name = "slf4j-simple"),
    ExclusionRule(organization = "org.mockito", name = "mockito-all"),
    ExclusionRule(organization = "org.hamcrest", name = "hamcrest-core")
    ),
  "chronosQ" % "chronosQ-reservation-impl-hbase" % "1.1-SNAPSHOT" excludeAll(
    ExclusionRule("org.slf4j"),
    ExclusionRule("io.netty"),
    ExclusionRule("asm"),
    ExclusionRule("log4j"),
    ExclusionRule("org.apache.curator"),
    ExclusionRule(organization = "com.codahale.metrics"),
    ExclusionRule(organization = "org.mockito", name = "mockito-all"),
    ExclusionRule(organization = "org.hamcrest", name = "hamcrest-core")
    )
)

assemblyMergeStrategy in assembly := AppBuild.mergeStrategy

test in assembly := {}
