name := "busybees"

version := "0.1"

libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.5",
  "org.igniterealtime.smack" % "smack-core" % "4.1.5",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.5",
  "org.isomorphism" % "token-bucket" % "1.6" excludeAll ExclusionRule("com.google.guava", "guava"),

  /** apns using pushy,  **/
  "com.relayrides" % "pushy" % "0.7.2" excludeAll ExclusionRule("io.netty"),
  "io.netty" % "netty-tcnative-openssl102" % "1.1.33.Fork14" classifier "linux-x86_64",
  "io.netty" % "netty-tcnative" % "1.1.33.Fork14" classifier "osx-x86_64",
  "org.eclipse.jetty.alpn" % "alpn-api" % "1.1.2.v20150522",
  /** pushy dependecy ends **/

  /** email **/
  "javax.mail" % "javax.mail-api" % "1.5.6"
  /** email-end **/
)


test in assembly := {}

parallelExecution in Test := false

assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {_.data.getName == "netty-tcnative-1.1.33.Fork14-osx-x86_64.jar"}
}


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy
