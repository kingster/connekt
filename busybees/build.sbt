name := "busybees"

version := "0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.0",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M1"
)


test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy