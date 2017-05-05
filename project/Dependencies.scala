import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "1.8.0-604-2128ff7b"
  val paradiseV = "3.0.0-308-ec15a2f8"
  val metaconfigV = "0.3.2"
  var testClasspath: String = "empty"
  def scalahost: ModuleID = "org.scalameta" % s"scalahost" % scalametaV cross CrossVersion.full
  def scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.0"
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def ammonite = "com.lihaoyi" %% "ammonite-ops" % "0.8.2"
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def scalameta: ModuleID =
    "org.scalameta" %% "contrib" % scalametaV excludeAll (
      ExclusionRule("org.scalameta", "testkit_2.11"),
      ExclusionRule("org.scalameta", "testkit_2.12"),
      ExclusionRule("org.apache.commons"),
      ExclusionRule("commons-io")
    )
}
