import ScalafixBuild.autoImport.isScala2
import sbt.Keys.scalaVersion
import sbt._

import scala.util.Try

/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scala212 = "2.12.17"
  val scala213 = "2.13.10"
  val scala3 = "3.2.2"

  val buildScalaVersions = Seq(scala212, scala213, scala3)
  val testTargetScalaVersions = Seq(scala212, scala213, scala3)

  // we support 3 last binary versions of scala212 and scala213
  val testedPreviousScalaVersions: Map[String, List[String]] =
    List(scala213, scala212).map(version => version -> previousVersions(version)).toMap

  val bijectionCoreV = "0.9.7"
  val collectionCompatV = "2.9.0"
  val coursierV = "2.1.0"
  val coursierInterfaceV = "1.0.14"
  val commontTextV = "1.10.0"
  val googleDiffV = "1.3.0"
  val java8CompatV = "0.9.1"
  val jgitV = "5.13.1.202206130422-r"
  val metaconfigV = "0.11.1"
  val pprintV = "0.6.6" // don't bump, rules built against metaconfig 0.9.15 or earlier would not link
  val nailgunV = "0.9.1"
  val scalaXmlV = "2.1.0"
  val scalametaV = "4.7.6"
  val scalatestMinV = "3.0.8" // don't bump, to avoid forcing breaking changes on clients via eviction
  val scalatestLatestV = "3.2.13"
  val munitV = "0.7.29"

  val bijectionCore = "com.twitter" %% "bijection-core" % bijectionCoreV
  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompatV
  val commonText = "org.apache.commons" % "commons-text" % commontTextV
  val coursier = ("io.get-coursier" %% "coursier" % coursierV)
    .cross(CrossVersion.for3Use2_13)
  val coursierInterfaces = "io.get-coursier" % "interface" % coursierInterfaceV
  val googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % googleDiffV
  val java8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % java8CompatV
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % jgitV
  val metaconfig = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  val pprint = "com.lihaoyi" %% "pprint" % pprintV
  val metaconfigDoc = "com.geirsson" %% "metaconfig-docs" % metaconfigV
  val metacp = "org.scalameta" %% "metacp" % scalametaV
  val nailgunServer = "com.martiansoftware" % "nailgun-server" % nailgunV
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaXmlV
  // https://github.com/scalameta/scalameta/issues/2485
  val scalameta = ("org.scalameta" %% "scalameta" % scalametaV)
    .cross(CrossVersion.for3Use2_13)
  val scalametaTeskit = ("org.scalameta" %% "testkit" % scalametaV)
    .cross(CrossVersion.for3Use2_13)
  val scalatest = "org.scalatest" %% "scalatest" % scalatestMinV
  val munit = "org.scalameta" %% "munit" % munitV
  val semanticdbScalacCore = "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full

  private def previousVersions(scalaVersion: String): List[String] = {
    val split = scalaVersion.split('.')
    val binaryVersion = split.take(2).mkString(".")
    val compilerVersion = Try(split.last.toInt).toOption
    val previousPatchVersions =
      compilerVersion.map(version => List.range(version - 7, version).filter(_ >= 0)).getOrElse(Nil)
    previousPatchVersions.map(v => s"$binaryVersion.$v")
  }
}
