import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {

  val scalametaV = "2.0.1"
  val metaconfigV = "0.5.2"
  def semanticdbSbt = "0.4.0"
  def dotty = "0.1.1-bin-20170530-f8f52cc-NIGHTLY"
  def scala210 = "2.10.6"
  def scala211 = "2.11.11"
  def scala212 = "2.12.3"
  def sbt013 = "0.13.6"
  def sbt1 = "1.0.2"
  def ciScalaVersion = sys.env.get("CI_SCALA_VERSION")

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.9.0"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def scalameta = Def.setting("org.scalameta" %%% "contrib" % scalametaV)
  def scalatest = Def.setting("org.scalatest" %%% "scalatest" % "3.0.0")
  def utest = Def.setting("com.lihaoyi" %%% "utest" % "0.6.0")

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
