import sbt._
/* scalafmt: {
maxColumn = 120
style = defaultWithAlign
}*/

object Dependencies {
  val scalametaV = "1.6.0"
  val paradiseV  = "3.0.0-M7"
  var testClasspath: String = "empty"
  def scalahost(scalaVersion: String): ModuleID = "org.scalameta" % s"scalahost_$scalaVersion" % scalametaV
  def scalameta: ModuleID                       = "org.scalameta" %% "contrib"                 % scalametaV
  def scalatest: ModuleID                       = "org.scalatest" %% "scalatest"               % "3.0.0"
  def metaconfig: ModuleID                      = "com.geirsson"  %% "metaconfig-hocon"        % "0.1.2"
  def ammonite                                  = "com.lihaoyi"   %% "ammonite-ops"            % "0.8.2"
}
