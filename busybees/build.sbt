name := "busybees"

version := "0.1"

libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.5",
  "org.igniterealtime.smack" % "smack-core" % "4.1.5",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.5",
  "org.isomorphism" % "token-bucket" % "1.6",
  "com.notnoop.apns" % "apns" % "1.0.0.Beta6"
)


test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy