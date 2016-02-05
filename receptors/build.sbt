name := "receptors"

version := "0.1"

libraryDependencies ++= Seq(
/*
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0-M1",
*/
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.3"
)

test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy