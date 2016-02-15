import sbt.Keys._

/** Project */
organization := "com.flipkart.marketing"

name := "connekt-8087"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

publishMavenStyle := true

publishTo := {
  val flipkart = "http://artifactory.nm.flipkart.com:8081/artifactory/"
  if (isSnapshot.value)
    Some("Flipkart Repo Snapshots" at flipkart + "libs-snapshot-local")
  else
    Some("Flipkart Repo Releases"  at flipkart + "libs-release-local")
}

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value,
  "junit" % "junit" % "4.8.1" % Test,
  "org.scala-tools.testing" % "specs_2.10" % "1.6.9" % Test,
  "org.apache.hbase" % "hbase" % "0.94.15-cdh4.7.0" excludeAll(
    ExclusionRule(organization = "com.sun.jersey")
  ),
  "org.glassfish.jersey.core" % "jersey-server" % "2.22.1",
  "org.glassfish.jersey.media" % "jersey-media-json-jackson" % "2.22.1"
)

