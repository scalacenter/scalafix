import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "4.3.0"
  val metaconfigV = "0.9.4"
  def scala210 = "2.10.7"
  def scala211 = "2.11.12"
  def scala212 = "2.12.10"
  def scala213 = "2.13.1"
  val currentScalaVersion = scala212

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "5.5.1.201910021850-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def semanticdbPluginLibrary = "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  def scalameta = "org.scalameta" %% "scalameta" % scalametaV
  def scalatest = "org.scalatest" %% "scalatest" % "3.1.0"
  def scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.3"

  def testsDeps = List(
    // integration property tests
    "io.get-coursier" %% "coursier" % "2.0.0-RC3-3",
    "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "com.typesafe.slick" %% "slick" % "3.3.2",
    "com.chuusai" %% "shapeless" % "2.3.3",
    scalacheck
  )

  def coursierDeps = Seq(
    "io.get-coursier" %% "coursier" % coursier.util.Properties.version,
    "io.get-coursier" %% "coursier-cache" % coursier.util.Properties.version
  )
}
