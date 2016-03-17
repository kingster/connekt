name := "receptors"

version := "0.1"

libraryDependencies ++= Seq()

test in assembly := {}

parallelExecution in Test := false


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy
