import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "1.7.0"
  val paradiseV = "3.0.0-M8"

  var testClasspath: String = "empty"
  def scalahost: ModuleID = "org.scalameta" % s"scalahost" % scalametaV cross CrossVersion.full
  def scalahostNsc: ModuleID = "org.scalameta" % s"scalahost-nsc" % scalametaV cross CrossVersion.full
  def scalameta: ModuleID = "org.scalameta" %% "contrib" % scalametaV
  def scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.0"
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % "0.3.1"
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.8.2"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
}
