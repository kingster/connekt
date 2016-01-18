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
  "org.scala-lang" % "scala-library" % "2.10.4",
  "junit" % "junit" % "4.8.1" % Test,
  "org.scala-tools.testing" % "specs_2.10" % "1.6.9" % Test,
  "org.apache.hbase" % "hbase" % "0.94.15-cdh4.7.0"
)

