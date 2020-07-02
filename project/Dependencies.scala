import sbt._

import scala.util.{Properties, Try}
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "4.3.18"
  val metaconfigV = "0.9.10"
  def scala210 = "2.10.7"
  def scala211 = "2.11.12"
  def scala212 = "2.12.11"
  def scala213 = "2.13.3"
  def coursierV = "2.0.0-RC5-6"
  def coursierInterfaceV = "0.0.22"
  val currentScalaVersion = scala213
  // we support 3 last binary versions of scala212 and scala213
  val testedPreviousScalaVersions =
    List(scala213, scala212).map(version => version -> previousVersions(version)).toMap

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "5.8.0.202006091008-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def semanticdbPluginLibrary = "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  def scalameta = "org.scalameta" %% "scalameta" % scalametaV
  def scalatest =
    "org.scalatest" %% "scalatest" % "3.0.8" // don't bump, to avoid forcing breaking changes on clients via eviction
  def bijectionCore = "com.twitter" %% "bijection-core" % "0.9.7"
  def scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.3"
  def collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6"

  def testsDeps = List(
    // integration property tests
    "io.get-coursier" %% "coursier" % coursierV,
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "com.typesafe.slick" %% "slick" % "3.3.2",
    "com.chuusai" %% "shapeless" % "2.3.3",
    scalacheck
  )

  private def previousVersions(scalaVersion: String): List[String] = {
    val split = scalaVersion.split('.')
    val binaryVersion = split.take(2).mkString(".")
    val compilerVersion = Try(split.last.toInt).toOption
    val previousPatchVersions =
      compilerVersion.map(version => List.range(version - 2, version).filter(_ >= 0)).getOrElse(Nil)
    previousPatchVersions.map(v => s"$binaryVersion.$v")
  }
}
