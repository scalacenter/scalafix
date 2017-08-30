import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {

  val scalametaV = "2.0.0-M3"
  val metaconfigV = "0.5.2"
  def sbthostV = "0.3.1"
  def dotty = "0.1.1-bin-20170530-f8f52cc-NIGHTLY"
  def scala210 = "2.10.6"
  def scala211 = "2.11.11"
  def scala212 = "2.12.3"
  def ciScalaVersion = sys.env.get("CI_SCALA_VERSION")

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.9.0"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def scalameta = Def.setting("org.scalameta" %%% "contrib" % scalametaV)
  def scalatest = Def.setting("org.scalatest" %%% "scalatest" % "3.0.0")
}
