import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "1.8.0"
  val paradiseV = "3.0.0-M9"
  val metaconfigV = "0.3.3"

  var testClasspath: String = "empty"
  def scalahost: ModuleID = "org.scalameta" % s"scalahost" % scalametaV cross CrossVersion.full
  def scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.0"
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.9.0"
  def fastparse = "com.lihaoyi" %% "fastparse" % "0.4.3"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def scalameta: ModuleID = "org.scalameta" %% "contrib" % scalametaV
}
