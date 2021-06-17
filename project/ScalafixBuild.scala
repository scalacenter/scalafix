import Dependencies._
import sbt._
import sbt.Keys._
import sbt.nio.Keys._
import sbt.plugins.JvmPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoPlugin.autoImport.{BuildInfoKey, _}
import com.typesafe.sbt.sbtghpages.GhpagesKeys
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbt.plugins.IvyPlugin

object ScalafixBuild extends AutoPlugin with GhpagesKeys {
  override def trigger = allRequirements
  override def requires = JvmPlugin && IvyPlugin
  object autoImport {
    lazy val stableVersion =
      settingKey[String]("Version of latest release to Maven.")
    lazy val noPublishAndNoMima = Seq(
      mimaReportBinaryIssues := {},
      mimaPreviousArtifacts := Set.empty,
      publish / skip := true
    )
    lazy val supportedScalaVersions = List(scala213, scala211, scala212)
    lazy val publishLocalTransitive =
      taskKey[Unit]("Run publishLocal on this project and its dependencies")
    lazy val crossPublishLocalBinTransitive = taskKey[Unit](
      "Run, for each crossVersion, publishLocal without packageDoc & packageSrc, on this project and its dependencies"
    )
    lazy val isFullCrossVersion = Seq(
      crossVersion := CrossVersion.full
    )
    lazy val isScala3 = Def.setting {
      scalaVersion.value.startsWith("3")
    }
    lazy val isScala2 = Def.setting {
      scalaVersion.value.startsWith("2")
    }
    lazy val isScala213 = Def.setting {
      scalaVersion.value.startsWith("2.13")
    }
    lazy val isScala212 = Def.setting {
      scalaVersion.value.startsWith("2.12")
    }
    lazy val isScala211 = Def.setting {
      scalaVersion.value.startsWith("2.11")
    }
    lazy val warnUnusedImports = Def.setting {
      if (isScala3.value) Nil
      else if (isScala213.value) Seq("-Wunused:imports")
      else Seq("-Ywarn-unused-import")
    }
    lazy val warnUnused = Def.setting {
      if (isScala2.value) Seq("-Ywarn-unused")
      else Nil
    }
    lazy val targetJvm = Def.setting {
      if (isScala3.value) "-Xtarget:8"
      else "-target:jvm-1.8"
    }
    val warnAdaptedArgs = Def.setting {
      if (isScala3.value) Nil
      else if (isScala213.value) Seq("-Xlint:adapted-args")
      else Seq("-Ywarn-adapted-args")
    }
    lazy val maxwarns = Def.setting {
      if (isScala213.value || isScala212.value) Seq("-Xmaxwarns", "2000")
      else Nil
    }
    lazy val scaladocOptions = Seq(
      "-groups",
      "-implicits"
    )
    lazy val testsDependencies = Def.setting {
      val xmlLib = if (isScala211.value) scalaXml211 else scalaXml
      val otherLibs =
        if (isScala2.value)
          Seq(
            bijectionCore,
            "org.scala-lang" % "scala-reflect" % scalaVersion.value
          )
        else Nil
      xmlLib +: otherLibs
    }
    lazy val compilerOptions = Def.setting(
      warnUnusedImports.value ++
        Seq(
          targetJvm.value,
          "-encoding",
          "UTF-8",
          "-feature",
          "-unchecked"
        )
    )

    lazy val buildInfoSettingsForCore: Seq[Def.Setting[_]] = Seq(
      buildInfoKeys := Seq[BuildInfoKey](
        name,
        version,
        stableVersion,
        "coursier" -> coursierV,
        "nightly" -> version.value,
        "scalameta" -> scalametaV,
        scalaVersion,
        "supportedScalaVersions" -> supportedScalaVersions,
        "scala211" -> scala211,
        "scala212" -> scala212,
        "scala213" -> scala213,
        sbtVersion
      ),
      buildInfoPackage := "scalafix",
      buildInfoObject := "Versions"
    )

    lazy val buildInfoSettingsForRules: Seq[Def.Setting[_]] = Seq(
      buildInfoKeys ++= Seq[BuildInfoKey](
        "supportedScalaVersions" -> (scalaVersion.value +:
          testedPreviousScalaVersions
            .getOrElse(scalaVersion.value, Nil)),
        "allSupportedScalaVersions" -> ((crossScalaVersions.value ++ testedPreviousScalaVersions.values.toSeq.flatten).sorted)
      ),
      buildInfoObject := "RulesBuildInfo"
    )
  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = List(
    stableVersion := (ThisBuild / version).value.replaceFirst("\\+.*", ""),
    resolvers ++= List(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("public"),
      Resolver.mavenLocal
    ),
    Test / testOptions += Tests.Argument("-oD"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger,
    commands += Command.command("save-expect") { s =>
      "unit/test:runMain scalafix.tests.util.SaveExpect" ::
        s
    },
    commands += Command.command("ci-3") { s =>
      s"""set testsInput/scalaVersion := "$scala3"""" ::
        s"""set testsOutput/scalaVersion := "$scala3"""" ::
        "unit/testOnly scalafix.tests.rule.RuleSuite" :: s
    },
    commands += Command.command("ci-213") { s =>
      s"""set ThisBuild/scalaVersion := "$scala213"""" ::
        "unit/test" ::
        "docs/run" ::
        "interfaces/doc" ::
        testRulesAgainstPreviousScalaVersions(scala213, s)
    },
    commands += Command.command("ci-212") { s =>
      s"""set ThisBuild/scalaVersion := "$scala212"""" ::
        "unit/test" ::
        testRulesAgainstPreviousScalaVersions(scala212, s)
    },
    commands += Command.command("ci-211") { s =>
      s"""set ThisBuild/scalaVersion := "$scala211"""" ::
        "unit/test" ::
        testRulesAgainstPreviousScalaVersions(scala211, s)
    },
    commands += Command.command("ci-213-windows") { s =>
      s"++$scala213" ::
        "cli/crossPublishLocalBinTransitive" :: // scalafix.tests.interfaces.ScalafixSuite
        s"unit/testOnly -- -l scalafix.internal.tests.utils.SkipWindows" ::
        s
    },
    // There is flakyness in CliGitDiffTests and CliSemanticTests
    Test / parallelExecution := false,
    Test / publishArtifact := false,
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalacenter/scalafix")),
    autoAPIMappings := true,
    apiURL := Some(url("https://scalacenter.github.io/scalafix/")),
    organization := "ch.epfl.scala",
    developers ++= Developers.list
  )

  private val PreviousScalaVersion: Map[String, String] = Map(
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    scalacOptions ++= compilerOptions.value,
    Compile / console / scalacOptions :=
      compilerOptions.value :+ "-Yrepl-class-based",
    Compile / doc / scalacOptions ++= scaladocOptions,
    publishTo := Some {
      if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    },
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalacenter/scalafix"),
        "scm:git:git@github.com:scalacenter/scalafix.git"
      )
    ),
    mimaPreviousArtifacts := {
      val currentScalaFullV = scalaVersion.value
      val previousScalaFullV =
        PreviousScalaVersion.getOrElse(currentScalaFullV, currentScalaFullV)
      val previousScalaVCrossName = CrossVersion(
        crossVersion.value,
        previousScalaFullV,
        scalaBinaryVersion.value
      ).getOrElse(identity[String] _)(moduleName.value)
      Set(
        organizationName.value % previousScalaVCrossName % stableVersion.value
      )
    },
    mimaDependencyResolution := {
      // effectively reverts https://github.com/lightbend/mima/pull/508 since the
      // Coursier resolution ignores/overrides the explicit scala full version set
      // in mimaPreviousArtifacts
      val ivy = sbt.Keys.ivySbt.value
      IvyDependencyResolution(ivy.configuration)
    },
    mimaBinaryIssueFilters ++= Mima.ignoredABIProblems,
    publishLocalTransitive := Def.taskDyn {
      val ref = thisProjectRef.value
      publishLocal.all(ScopeFilter(inDependencies(ref)))
    }.value,
    crossPublishLocalBinTransitive := {
      val currentState = state.value
      val ref = thisProjectRef.value
      val versions = crossScalaVersions.value
      versions.map { version =>
        val withScalaVersion = Project
          .extract(currentState)
          .appendWithoutSession(
            Seq(
              ThisBuild / scalaVersion := version,
              ThisBuild / packageDoc / publishArtifact := false,
              ThisBuild / packageSrc / publishArtifact := false
            ),
            currentState
          )
        Project
          .extract(withScalaVersion)
          .runTask(ref / publishLocalTransitive, withScalaVersion)
      }
    }
  )
  private def testRulesAgainstPreviousScalaVersions(
      scalaVersion: String,
      state: State
  ): State = {
    testedPreviousScalaVersions
      .getOrElse(scalaVersion, Nil)
      .flatMap { v =>
        List(
          s"""set testsInput/scalaVersion := "$v"""",
          "show testsInput/scalaVersion",
          s"unit/testOnly scalafix.tests.rule.RuleSuite"
        )
      }
      .foldRight(state)(_ :: _)
  }
}
