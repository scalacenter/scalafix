import sbt._
/* scalafmt: {
maxColumn = 120
style = defaultWithAlign
}*/

object Dependencies {
  val scalametaV = "1.7.0-496-5f890293"
  val paradiseV  = "3.0.0-300-0dbf9cb7"

  var testClasspath: String = "empty"
  def scalahost: ModuleID    = "org.scalameta" % s"scalahost"        % scalametaV cross CrossVersion.full
  def scalahostNsc: ModuleID = "org.scalameta" % s"scalahost-nsc"    % scalametaV cross CrossVersion.full
  def scalameta: ModuleID    = "org.scalameta" %% "contrib"          % scalametaV
  def scalatest: ModuleID    = "org.scalatest" %% "scalatest"        % "3.0.0"
  def metaconfig: ModuleID   = "com.geirsson"  %% "metaconfig-hocon" % "0.2.1"
  def ammonite               = "com.lihaoyi"   %% "ammonite-ops"     % "0.8.2"
}
