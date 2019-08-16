import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "4.2.1"
  val metaconfigV = "0.9.1"
  def dotty = "0.9.0-RC1"
  def scala210 = "2.10.6"
  def scala211 = "2.11.12"
  def scala212 = "2.12.8"
  val currentScalaVersion = scala212

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.4.201711221230-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def semanticdbPluginLibrary = "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  def scalameta = "org.scalameta" %% "scalameta" % scalametaV
  def scalatest = "org.scalatest" %% "scalatest" % "3.2.0-SNAP10"
  def scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  def testsDeps = List(
    // integration property tests
    "com.geirsson" %% "coursier-small" % "1.0.0-M4",
    "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
    "org.typelevel" %% "catalysts-platform" % "0.0.5",
    "org.typelevel" %% "cats-core" % "0.9.0",
    "com.typesafe.slick" %% "slick" % "3.2.0-M2",
    "com.chuusai" %% "shapeless" % "2.3.2",
    scalacheck
  )

  def coursierDeps = Seq(
    "io.get-coursier" %% "coursier" % coursier.util.Properties.version,
    "io.get-coursier" %% "coursier-cache" % coursier.util.Properties.version
  )
}
