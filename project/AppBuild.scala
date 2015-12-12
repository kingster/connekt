import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.Plugin._
import sbt._
import sbt.Keys._

object AppBuild extends Build {

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
      "-Ywarn-adapted-args"
    ),
    crossPaths in Scope.GlobalScope := false,
    resolvers ++= Seq(
      "Maven2 Local" at Resolver.mavenLocal.root,
      "spray repo" at "http://repo.spray.io/",
      "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype release" at "https://oss.sonatype.org/content/repositories/releases",
      "flipkart local-releases" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-releases-local",
      "flipkart ext-releases" at "http://artifactory.nm.flipkart.com:8081/artifactory/ext-releases-local",
      "flipkart central" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-release",
      "flipkart snapshorts" at "http://artifactory.nm.flipkart.com:8081/artifactory/libs-snapshot",
      "flipkart clojars" at "http://artifactory.nm.flipkart.com:8081/artifactory/clojars-repo",
      "cloudera" at "https://repository.cloudera.com/artifactory/repo/",
      "cloudera releases" at "https://repository.cloudera.com/content/repositories/releases/",
      "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      "jBoss" at "http://repository.jboss.org/nexus/content/groups/public",
      "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
    ),
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
  )

  lazy val root =
    Project("root", file("."), settings = _commonSettings)
      .aggregate(receptors, busybees, commons)
      .dependsOn(receptors, busybees, commons)

  lazy val commons = Project("commons", file("commons"), settings=_commonSettings)

  lazy val receptors = Project("receptors", file("receptors"), settings=_commonSettings)
    .dependsOn(commons % "test->test;compile->compile")


  lazy val busybees = Project("busybees", file("busybees"), settings=_commonSettings)
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
    case PathList("javax", "xml", "namespace" , xs@_ *) => MergeStrategy.first  //fuck u xpp3

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
        case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") || ps.last.endsWith("pom.properties") =>
          MergeStrategy.discard
        case "plexus" :: xs =>
          MergeStrategy.discard
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case ps@(x :: xs) if ps.last.endsWith("jersey-module-version") || ps.last.endsWith("pom.xml") =>
          MergeStrategy.last
        case _ =>
          // Go to hell
          //MergeStrategy.deduplicate
          MergeStrategy.last
      }
    case _ =>
      MergeStrategy.deduplicate
  }
}