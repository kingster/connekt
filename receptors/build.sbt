name := "receptors"

version := "0.1"

libraryDependencies ++= Seq()

test in assembly := {}

parallelExecution in Test := false

assemblyMergeStrategy in assembly := AppBuild.mergeStrategy

libraryDependencies ++= Seq(
  "org.jboss.aerogear" % "aerogear-otp-java" % "1.0.0" withSources(),
  "com.google.api-client" % "google-api-client" % "1.19.1"

)
