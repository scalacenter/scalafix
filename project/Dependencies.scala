import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "3.7.0"
  val metaconfigV = "0.6.0-RC1"
  def dotty = "0.1.1-bin-20170530-f8f52cc-NIGHTLY"
  def scala210 = "2.10.6"
  // NOTE(olafur) downgraded from 2.11.12 and 2.12.4 because of non-reproducible error
  // https://travis-ci.org/scalacenter/scalafix/jobs/303142842#L4658
  // as well as https://github.com/scala/bug/issues/10609
  def scala211 = "2.11.12"
  def scala212 = "2.12.4"
  val currentScalaVersion = scala212

  def sbt013 = "0.13.16"
  def sbt1 = "1.0.4"
  val currentSbtVersion = sbt1

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.4.201711221230-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.9.0"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def scalameta = Def.setting("org.scalameta" %%% "contrib" % scalametaV)
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0"

  def testsDeps = List(
    // integration property tests
    "org.renucci" %% "scala-xml-quote" % "0.1.4",
    "org.typelevel" %% "catalysts-platform" % "0.0.5",
    "org.typelevel" %% "cats-core" % "0.9.0",
    "com.typesafe.slick" %% "slick" % "3.2.0-M2",
    "com.chuusai" %% "shapeless" % "2.3.2",
    "org.scalacheck" %% "scalacheck" % "1.13.4"
  )

  def coursierDeps = Seq(
    "io.get-coursier" %% "coursier" % coursier.util.Properties.version,
    "io.get-coursier" %% "coursier-cache" % coursier.util.Properties.version
  )
}
