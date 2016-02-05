name := "busybees"

version := "0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.0",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M1",
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.5",
  "org.igniterealtime.smack" % "smack-core" % "4.1.5",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.5",
  "org.isomorphism" % "token-bucket" % "1.6",
  "com.notnoop.apns" % "apns" % "0.2.3"

)


test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy