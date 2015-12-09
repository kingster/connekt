import com.typesafe.sbt.SbtStartScript

organization  := "com.flipkart.marketing"

name := "fk-pf-connekt"

version := "1.0"

parallelExecution in Test := false

assembleArtifact in assemblyPackageScala := true

assemblyJarName in assembly := "fk-pf-connekt.jar"

assemblyMergeStrategy in assembly :=  AppBuild.mergeStrategy

mainClass in assembly := Some("com.flipkart.connekt.boot") // fully qualified path

test in assembly := {}

seq(Revolver.settings: _*)

seq(SbtStartScript.startScriptForJarSettings: _*)


