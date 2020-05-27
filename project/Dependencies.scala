import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "4.3.10"
  val metaconfigV = "0.9.10"
  def scala210 = "2.10.7"
  def scala211 = "2.11.12"
  def scala212 = "2.12.11"
  def scala213 = "2.13.2"
  def coursierV = "2.0.0-RC5-6"
  val currentScalaVersion = scala213

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "5.7.0.202003110725-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def semanticdbPluginLibrary = "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  def scalameta = "org.scalameta" %% "scalameta" % scalametaV
  def scalatest = "org.scalatest" %% "scalatest" % "3.0.8"
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
}

object versions {
  val value = "0.9.15.2-SNAPSHOT"
}
