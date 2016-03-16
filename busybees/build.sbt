name := "busybees"

version := "0.1"

libraryDependencies ++= Seq(
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.5",
  "org.igniterealtime.smack" % "smack-core" % "4.1.5",
  "org.igniterealtime.smack" % "smack-extensions" % "4.1.5",
  "org.isomorphism" % "token-bucket" % "1.6",

  /** apns using pushy,  **/                                                            // --+
  "com.relayrides" % "pushy" % "0.5.2" excludeAll(                                      //   |
    ExclusionRule(organization = "io.netty", name = "netty-codec-http2"),               //   | - Do not touch this, or prepare for
    ExclusionRule(organization = "io.netty", name = "netty-handler-proxy")              //   |   the sky to fall on your head!
    ),                                                                                  //   |
  "io.netty" % "netty-codec-http2" % "4.1.0.CR3",                                       //   |
  "io.netty" % "netty-handler-proxy" % "4.1.0.CR3",                                     //   |
  "io.netty" % "netty-tcnative-openssl102" % "1.1.33.Fork14" classifier "linux-x86_64", //   |
  "io.netty" % "netty-tcnative" % "1.1.33.Fork14" classifier "osx-x86_64",              // --+
  "org.eclipse.jetty.alpn" % "alpn-api" % "1.1.2.v20150522"
  /** pushy dependecy ends **/
  /* "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.8.5" */
)


test in assembly := {}

parallelExecution in Test := false

assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {_.data.getName == "netty-tcnative-1.1.33.Fork14-osx-x86_64.jar"}
}


assemblyMergeStrategy in assembly := AppBuild.mergeStrategy
