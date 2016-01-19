logLevel := Level.Warn

resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
resolvers += Resolver.url("kinshuk-bintray", url("https://dl.bintray.com/kingster/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" %  "0.10.0")

addSbtPlugin("org.scala-sbt.plugins" % "sbt-onejar" % "0.8")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")

//addSbtPlugin("com.flipkart.jira" % "sbt-jira-mandate" % "1.0.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

addSbtPlugin("com.aol.sbt" % "sbt-sonarrunner-plugin" % "1.0.4")

