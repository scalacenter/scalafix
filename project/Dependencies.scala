import sbt.*

import scala.language.implicitConversions

/* scalafmt: { maxColumn = 140 }*/

object Dependencies {
  // upon bump, remove JDK25 exclusion in cliScalaVersions & in ScalafixSuite
  val scala212 = sys.props.getOrElse("scala212.nightly", "2.12.21")
  val scala213 = sys.props.getOrElse("scala213.nightly", "2.13.18")
  val scala33 = "3.3.7"
  val scala35 = "3.5.2"
  val scala36 = "3.6.4"
  val scala37 = "3.7.4"
  val scala38 = "3.8.3" // Upon bump, check if javaEnumWidening is still needed (https://github.com/scala/scala3/pull/25469)
  val scala3LTS = scala33
  val scala3Next = sys.props.getOrElse("scala3.nightly", scala38)

  val scala2Versions = Seq(scala212, scala213)
  val scala3Versions = Seq(scala33, scala35, scala36, scala37, scala38)

  val bijectionCoreV = "0.9.8"
  val collectionCompatV = "2.14.0"
  val coursierV = "2.1.24"
  val coursierInterfaceV = "1.0.28"
  val commontTextV = "1.15.0"
  val googleDiffV = "1.3.0"
  val jgitV = "5.13.5.202508271544-r"
  val metaconfigV = "0.18.5"
  val nailgunV = "0.9.1"
  val scalaXmlV = "2.4.0"
  val scalametaV = "4.16.1+5-71740a84-SNAPSHOT"
  val scalatagsV = "0.13.1"
  val scalatestV = "3.2.20"
  val munitV = "1.3.0"

  val orgScalafix = "ch.epfl.scala"
  val orgScalameta = "org.scalameta"
  val orgLiHaoYi = "com.lihaoyi"
  val orgScalaLang = "org.scala-lang"
  val orgScalaLangMod = "org.scala-lang.modules"

  val bijectionCore = "com.twitter" %% "bijection-core" % bijectionCoreV
  val collectionCompat = orgScalaLangMod %% "scala-collection-compat" % collectionCompatV
  val commonText = "org.apache.commons" % "commons-text" % commontTextV
  val coursierFor3Use2_13 = "io.get-coursier" %% "coursier" % coursierV cross CrossVersion.for3Use2_13
  val coursierInterfaces = "io.get-coursier" % "interface" % coursierInterfaceV
  val googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % googleDiffV
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % jgitV
  val metaconfig = orgScalameta %% "metaconfig-typesafe-config" % metaconfigV
  val metacp = orgScalameta %% "metacp" % scalametaV
  val nailgunServer = "com.martiansoftware" % "nailgun-server" % nailgunV
  val scalaXml = orgScalaLangMod %% "scala-xml" % scalaXmlV
  val scalameta = orgScalameta %% "scalameta" % scalametaV
  val scalametaTestkit = orgScalameta %% "testkit" % scalametaV
  val scalatags = orgLiHaoYi %% "scalatags" % scalatagsV
  val scalatest = "org.scalatest" %% "scalatest" % scalatestV
  val munit = orgScalameta %% "munit" % munitV
  val semanticdbScalac = orgScalameta % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  val semanticdbScalacCore = orgScalameta % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  val semanticdbShared = orgScalameta %% "semanticdb-shared" % scalametaV

  // scala-steward:off

  // Now shaded in dependencies, kept here only for backward compatbility
  val runtimeDepsForBackwardCompatibility = Seq(
    // metaconfig 0.10.0 shaded pprint
    // https://github.com/scalameta/metaconfig/pull/154/files#r794005161
    orgLiHaoYi %% "pprint" % "0.6.6",
    // scalameta 4.8.3 shaded fastparse and geny
    // https://github.com/scalameta/scalameta/pull/3246
    orgScalameta %% "fastparse-v2" % "2.3.1",
    orgLiHaoYi %% "geny" % "0.6.5"
  )

  implicit def moduleIDtoOrgName(obj: ModuleID): (String, String) =
    (obj.organization, obj.name)

  implicit class ImplicitModuleID(private val obj: ModuleID) extends AnyVal {
    def exclude213(other: (String, String)*): ModuleID = {
      val suffix = "_2.13"
      other.foldLeft(obj) { case (res, mod) =>
        res.exclude(mod._1, mod._2 + suffix)
      }
    }
  }

}
