import sbt._
import sbt.Keys._

object BuildCommons extends Build {
  lazy val _commonSettings = Seq[Def.Setting[_]](
    scalaVersion := "2.11.7",
    crossPaths in Scope.GlobalScope := false,
    resolvers ++= Seq(
      "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
    ),
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
  )

  lazy val root =
    Project("root", file("."), settings = _commonSettings)
      .aggregate(receptors, busybees, commons)

  lazy val commons = Project("commons", file("commons"), settings=_commonSettings)

  lazy val receptors = Project("receptors", file("receptors"), settings=_commonSettings)
    .dependsOn(commons % "test->test;compile->compile")
    .settings(
      mainClass in (Compile, run) := Some("com.flipkart.connekt.receptors.ReceptorsBoot")
    )

  lazy val busybees = Project("busybees", file("busybees"), settings=_commonSettings)
    .dependsOn(commons % "test->test;compile->compile")
    .settings(
      mainClass in (Compile, run) := Some("com.flipkart.connekt.busybees.BootBusyBees")
    )
}