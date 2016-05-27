import com.aol.sbt.sonar.SonarRunnerPlugin
import com.aol.sbt.sonar.SonarRunnerPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPattern}
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.Plugin._

object AppBuild extends Build  {

  lazy val _commonSettings = Seq[Def.Setting[_]](
    organization := "com.flipkart.marketing",
    scalaVersion := "2.11.7",
    scalacOptions := Seq(
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8",
      "-Ywarn-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
  ),
    crossPaths in Scope.GlobalScope := false,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      "Maven2 Local" at Resolver.mavenLocal.root,
      "spray repo" at "http://repo.spray.io/",
      "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype release" at "https://oss.sonatype.org/content/repositories/releases",
      "cloudera" at "https://repository.cloudera.com/artifactory/repo/",
      "cloudera releases" at "https://repository.cloudera.com/content/repositories/releases/",
      "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      "jBoss" at "http://repository.jboss.org/nexus/content/groups/public",
      "Akka Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
      "RoundEights" at "http://maven.spikemark.net/roundeights",
      "flipkart local-releases" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-releases-local",
      "flipkart ext-releases" at "http://artifactory.nm.flipkart.com:8081/artifactory/ext-release-local",
      "flipkart central" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-release",
      "flipkart snapshorts" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-snapshot",
      "flipkart clojars" at "http://artifactory.nm.flipkart.com:8081/artifactory/clojars-repo"
    ),
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    headers := headers.value ++ Map(
      "scala" -> (
        HeaderPattern.cStyleBlockComment,
        """|/*
           | *         -╥⌐⌐⌐⌐            -⌐⌐⌐⌐-
           | *      ≡╢░░░░⌐\░░░φ     ╓╝░░░░⌐░░░░╪╕
           | *     ╣╬░░`    `░░░╢┘ φ▒╣╬╝╜     ░░╢╣Q
           | *    ║╣╬░⌐        ` ╤▒▒▒Å`        ║╢╬╣
           | *    ╚╣╬░⌐        ╔▒▒▒▒`«╕        ╢╢╣▒
           | *     ╫╬░░╖    .░ ╙╨╨  ╣╣╬░φ    ╓φ░╢╢Å
           | *      ╙╢░░░░⌐"░░░╜     ╙Å░░░░⌐░░░░╝`
           | *        ``˚¬ ⌐              ˚˚⌐´
           | *
           | *      Copyright © 2016 Flipkart.com
           | */
           |""".stripMargin
        )
    ),
    updateOptions := updateOptions.value.withLatestSnapshots(false).withCachedResolution(true)
  )

  val envKey = SettingKey[String]("env-key", "Flipkart Environment.")

  // http://www.scala-sbt.org/0.13.5/docs/Howto/generatefiles.html#resources
  lazy val bareResourceGenerators: Seq[Setting[_]] = Seq(
    resourceGenerators in Compile += Def.task {

      var resourceFiles = Seq[File]()
      println(s"[info] Generating Runtime Resources for Environment [${envKey.value}]")
      val profileResourcesDir = baseDirectory.value / "src" / "main" / "alternate-resources" / envKey.value
      (profileResourcesDir * "*").get.foreach(f => {
        IO.copyFile(f, (resourceManaged in Compile).value / f.name)
        resourceFiles = resourceFiles :+ (resourceManaged in Compile).value / f.name
      })

      resourceFiles
    }.taskValue
  )

  lazy val buildInfoGenerator = Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, baseDirectory),
    buildInfoPackage := "com.flipkart.marketing.connekt"
  )

  lazy val root =
    Project("root", file("."))
      .enablePlugins(SonarRunnerPlugin)
      .settings(_commonSettings ++ Seq(
        publishArtifact := false,
        sonarRunnerOptions := Seq("-e")
      ): _*)
      .enablePlugins(AutomateHeaderPlugin)
      .dependsOn(receptors, busybees, commons)

  lazy val commons = Project("commons", file("commons"), settings = _commonSettings ++ buildInfoSettings ++
    buildInfoGenerator ++ bareResourceGenerators)
    .enablePlugins(AutomateHeaderPlugin)


  lazy val receptors = Project("receptors", file("receptors"), settings = _commonSettings)
    .enablePlugins(AutomateHeaderPlugin)
    .dependsOn(commons % "test->test;compile->compile")

  lazy val busybees = Project("busybees", file("busybees"), settings = _commonSettings)
    .enablePlugins(AutomateHeaderPlugin)
    .dependsOn(commons % "test->test;compile->compile")


  val mergeStrategy: String => sbtassembly.MergeStrategy = {
    case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first

    case PathList("com", "google", "common", xs@_ *) => MergeStrategy.last
    case PathList("com", "esotericsoftware", "reflectasm", xs@_ *) => MergeStrategy.last //screw u reflect asm. who & where are you?
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first // oh god upto you - javax stuff!
    case PathList("org", "apache", "commons", "collections", xs@_x) => MergeStrategy.first
    case PathList("org", "apache", "hadoop", xs@_ *) => MergeStrategy.last
    case PathList("org", "eclipse", "jetty", xs@_ *) => MergeStrategy.last
    //for anotations
    case PathList("edu", "umd", "cs", "findbugs", xs@_ *) => MergeStrategy.last
    case PathList("net", "jcip", xs@_ *) => MergeStrategy.last

    case PathList("org", "xmlpull", "v1", xs@_x) => MergeStrategy.first //crazy xpp3 people http://jira.codehaus.org/browse/XSTR-689
    case PathList("javax", "xml", "namespace", "QName.class") => new IncludeFromJar("xpp3-1.1.4c.jar") //fuck u xpp3

    case "mapred-default.xml" | "logback.xml" => MergeStrategy.first
    case "application.conf" | "plugin.properties" => MergeStrategy.concat

    case x if Assembly.isConfigFile(x) =>
      MergeStrategy.concat
    case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case PathList(ps@_*) if Assembly.isSystemJunkFile(ps.last) =>
      MergeStrategy.discard
    case PathList("META-INF", xs@_*) =>
      xs.map(_.toLowerCase) match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps@(x :: other) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") || ps.last.endsWith("pom.properties") =>
          MergeStrategy.discard
        case "plexus" :: other =>
          MergeStrategy.discard
        case "services" :: other =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case ps@(x :: other) if ps.last.endsWith("jersey-module-version") || ps.last.endsWith("pom.xml") =>
          MergeStrategy.last
        case ps@(x :: other) if ps.last.endsWith("RSA") ||  ps.last.endsWith("SF") || ps.last.endsWith("DSA") =>
            MergeStrategy.discard
        case _ =>
          // Go to hell
          //MergeStrategy.deduplicate
          MergeStrategy.last
      }
    case _ =>
      MergeStrategy.deduplicate
  }
}
