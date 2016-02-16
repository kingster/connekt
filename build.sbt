import com.typesafe.sbt.SbtStartScript
import JiraIdPlugin.autoImport._

organization  := "com.flipkart.marketing"

name := "fk-pf-connekt"

version := "1.0"

parallelExecution in Test := false

assembleArtifact in assemblyPackageScala := true

assemblyJarName in assembly := "fk-pf-connekt.jar"

/** god knows why sbt requires this again here */
assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {_.data.getName == "netty-tcnative-1.1.33.Fork10-osx-x86_64.jar"}
}

assemblyMergeStrategy in assembly :=  AppBuild.mergeStrategy

mainClass in assembly := Some("com.flipkart.connekt.boot") // fully qualified path

test in assembly := {}

seq(Revolver.settings: _*)

seq(SbtStartScript.startScriptForJarSettings: _*)

jira := "CNKT"

seq(JiraIdPlugin.settings: _*)