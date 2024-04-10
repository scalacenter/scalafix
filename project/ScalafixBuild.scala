import Dependencies._
import sbt._
import sbt.Classpaths
import sbt.Keys._
import sbt.internal.ProjectMatrix
import sbt.nio.Keys._
import sbt.plugins.JvmPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtversionpolicy.SbtVersionPolicyPlugin
import sbtversionpolicy.SbtVersionPolicyPlugin.autoImport._
import scalafix.sbt.ScalafixPlugin.autoImport._
import com.github.sbt.sbtghpages.GhpagesKeys
import sbt.librarymanagement.ivy.IvyDependencyResolution
import sbt.plugins.IvyPlugin
import sbtversionpolicy.DependencyCheckReport
import scala.util.Try

object ScalafixBuild extends AutoPlugin with GhpagesKeys {
  override def trigger = allRequirements
  override def requires =
    JvmPlugin &&
      SbtVersionPolicyPlugin // don't let it override our mimaPreviousArtifacts
  object autoImport {
    lazy val stableVersion =
      settingKey[String]("Version of latest release to Maven.")
    lazy val noPublishAndNoMima = Seq(
      mimaReportBinaryIssues := {},
      mimaPreviousArtifacts := Set.empty,
      publish / skip := true
    )
    lazy val supportedScalaVersions = List(scala213, scala212)
    lazy val buildScalaVersions = Seq(scala212, scala213, scala3)
    lazy val buildScalaVersionsWithTargets: Seq[(String, TargetAxis)] =
      buildScalaVersions.map(sv => (sv, TargetAxis(sv))) ++
        Seq(scala213, scala212).flatMap { sv =>
          def previousVersions(scalaVersion: String): Seq[String] = {
            val split = scalaVersion.split('.')
            val binaryVersion = split.take(2).mkString(".")
            val compilerVersion = Try(split.last.toInt).toOption
            val previousPatchVersions =
              compilerVersion
                .map(version => List.range(version - 2, version).filter(_ >= 0))
                .getOrElse(Nil)
            previousPatchVersions
              .map { patch => s"$binaryVersion.$patch" }
              .filterNot { v =>
                System.getProperty("java.version").startsWith("21") &&
                Seq("2.12.17").contains(v)
              }
              .filterNot { v =>
                System.getProperty("java.version").startsWith("22") &&
                Seq("2.12.17", "2.12.18", "2.13.11", "2.13.12").contains(v)
              }
          }

          val prevVersions = previousVersions(sv).map(prev => TargetAxis(prev))
          val scala3FromScala2 = TargetAxis(scala3)
          val xsource3 = TargetAxis(sv, xsource3 = true)

          (prevVersions :+ scala3FromScala2 :+ xsource3).map((sv, _))
        }

    lazy val publishLocalTransitive =
      taskKey[Unit]("Run publishLocal on this project and its dependencies")
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
    lazy val warnUnused = Def.setting {
      if (isScala3.value) Seq("-Wunused:all", "-Wunused:unsafe-warn-patvars")
      else if (isScala213.value) Seq("-Wunused")
      else Seq("-Ywarn-unused")
    }
    lazy val targetJvm = Def.setting {
      if (isScala3.value) Seq("-release:8")
      else if (isScala213.value) Seq("-release", "8")
      else Seq("-target:jvm-1.8")
    }
    val warnAdaptedArgs = Def.setting {
      if (isScala3.value) Nil
      else if (isScala213.value) Seq("-Xlint:adapted-args", "-deprecation")
      else Seq("-Ywarn-adapted-args", "-deprecation")
    }
    lazy val scaladocOptions = Seq(
      "-groups",
      "-implicits"
    )
    lazy val testsDependencies = Def.setting {
      val otherLibs =
        if (isScala2.value)
          Seq(
            bijectionCore,
            "org.scala-lang" % "scala-reflect" % scalaVersion.value
          )
        else Nil
      scalaXml +: otherLibs
    }
    lazy val compilerOptions = Def.setting(
      targetJvm.value ++
        warnUnused.value ++
        Seq(
          "-encoding",
          "UTF-8",
          "-feature",
          "-unchecked"
        )
    )

    lazy val semanticdbSyntheticsCompilerOption = Def.setting(
      if (!isScala3.value)
        Seq("-P:semanticdb:synthetics:on")
      else Nil
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
        "scala212" -> scala212,
        "scala213" -> scala213,
        sbtVersion
      ),
      buildInfoPackage := "scalafix",
      buildInfoObject := "Versions"
    )

    lazy val buildInfoSettingsForRules: Seq[Def.Setting[_]] = Seq(
      buildInfoObject := "RulesBuildInfo"
    )

    lazy val testWindows =
      taskKey[Unit]("run tests, excluding those incompatible with Windows")

    /**
     * Lookup the project with the closest Scala version, and resolve `key`
     */
    def resolve[T](
        matrix: ProjectMatrix,
        key: SettingKey[T]
    ): Def.Initialize[T] =
      Def.settingDyn {
        val project = lookup(matrix, scalaVersion.value)
        Def.setting((project / key).value)
      }

    /**
     * Lookup the project with the closest Scala version, and resolve `key`
     */
    def resolve[T](
        matrix: ProjectMatrix,
        key: TaskKey[T]
    ): Def.Initialize[Task[T]] =
      Def.taskDyn {
        val project = lookup(matrix, scalaVersion.value)
        Def.task((project / key).value)
      }

  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = List(
    excludeLintKeys += scalafixConfig, // defined on projects where ScalafixPlugin is disabled
    stableVersion := (ThisBuild / version).value.replaceFirst("\\+.*", ""),
    resolvers ++=
      Resolver.sonatypeOssRepos("snapshots") ++
        Resolver.sonatypeOssRepos("public") :+
        Resolver.mavenLocal,
    Test / testOptions += Tests.Argument("-oD"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger,
    commands += Command.command("save-expect") { s =>
      "integration2_13/test:runMain scalafix.tests.util.SaveExpect" ::
        "integration3/test:runMain scalafix.tests.util.SaveExpect" ::
        s
    },
    commands += Command.command("ci-docs") { s =>
      "docs2_13/run" :: // reduce risk of errors on deploy-website.yml
        "interfaces/doc" ::
        s
    },
    commands += Command.command("dogfoodScalafixInterfaces") { state =>
      val extracted = Project.extract(state)
      val v =
        (ThisBuild / version)
          .get(extracted.structure.data)
          .get
      val suffix =
        (ThisBuild / scalafixScalaBinaryVersion)
          .get(extracted.structure.data)
          .get
          .replace('.', '_')

      s"all cli$suffix/publishLocalTransitive interfaces/publishLocal" ::
        "reload plugins" ::
        s"""set dependencyOverrides += "ch.epfl.scala" % "scalafix-interfaces" % "$v"""" :: // as documented in installation.md
        "session save" ::
        "reload return" ::
        state
    },
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

  override def buildSettings: Seq[Setting[_]] = List(
    // https://github.com/sbt/sbt/issues/5568#issuecomment-1094380636
    versionPolicyIgnored ++= Seq(
      // https://github.com/scalacenter/scalafix/pull/1530
      "com.lihaoyi" %% "pprint",
      // https://github.com/scalacenter/scalafix/pull/1819#issuecomment-1636118496
      "org.scalameta" %% "fastparse-v2",
      "com.lihaoyi" %% "geny"
    ),
    versionPolicyIgnoredInternalDependencyVersions :=
      Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r),
    versionScheme := Some("early-semver"),
    // coursier-versions always return false for the *.*.*.*-r pattern jgit uses
    libraryDependencySchemes += Dependencies.jgit.withRevision("always")
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    // Prevent issues with scalatest serialization
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    Test / testWindows := (Test / testOnly)
      .toTask(" -- -l scalafix.internal.tests.utils.SkipWindows")
      .value,
    // avoid "missing dependency" on artifacts with full scala version when bumping scala
    versionPolicyIgnored ++= {
      PreviousScalaVersion.get(scalaVersion.value) match {
        case Some(previous) =>
          // all transitive dependencies with full scala version we know about
          Seq(
            "org.scalameta" % s"semanticdb-scalac-core_$previous",
            "ch.epfl.scala" % s"scalafix-cli_$previous",
            "ch.epfl.scala" % s"scalafix-reflect_$previous"
          )
        case None => Seq()
      }
    },
    // don't publish scala 3 artifacts for now
    publish / skip := (if ((publish / skip).value) true
                       else scalaBinaryVersion.value == "3"),
    versionPolicyIntention := Compatibility.BinaryCompatible,
    scalacOptions += "-Wconf:origin=scala.collection.compat.*:s",
    scalacOptions ++= compilerOptions.value,
    scalacOptions ++= semanticdbSyntheticsCompilerOption.value,
    Compile / console / scalacOptions :=
      compilerOptions.value :+ "-Yrepl-class-based",
    Compile / doc / scalacOptions ++= scaladocOptions,
    Compile / unmanagedResourceDirectories ++= {
      val resourceParentDir = (Compile / resourceDirectory).value.getParentFile
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((major, _)) => Seq(resourceParentDir / s"resources-${major}")
        case _ => Seq()
      }
    },
    // Don't package sources & docs when publishing locally as it adds a significant
    // overhead when testing because of publishLocalTransitive. Tweaking publishArtifact
    // would more readable, but it would also affect remote (sonatype) publishing.
    publishLocal / packagedArtifacts :=
      Classpaths.packaged(Seq(Compile / packageBin)).value,
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
    mimaBinaryIssueFilters ++= Mima.ignoredABIProblems
  ) ++ Seq(Compile, Test).flatMap(conf => inConfig(conf)(configSettings))

  private def configSettings: Seq[Def.Setting[_]] = List(
    // Workaround for https://github.com/scalacenter/scalafix/issues/1592:
    // effectively skip scalafix[All] on 2.x projects not matching scalafixScalaBinaryVersion
    scalafix / unmanagedSources := {
      val prev = (scalafix / unmanagedSources).value
      if (
        Seq(scalafixScalaBinaryVersion.value, "3")
          .contains(scalaBinaryVersion.value)
      ) {
        prev
      } else {
        Seq()
      }
    },
    scalafixConfig := {
      if (scalaBinaryVersion.value.startsWith("2"))
        Some(file(".scalafix-scala2.conf"))
      else
        Some(file(".scalafix-scala3.conf"))
    }
  )

  /**
   * Find the project matching the full Scala version when available or a binary
   * one otherwise
   */
  private def lookup(matrix: ProjectMatrix, scalaVersion: String): Project = {
    val projects = matrix
      .allProjects()
      .collect {
        case (project, projectVirtualAxes)
            // CliSemanticSuite depends on classes compiled without -Xsource:3
            if !projectVirtualAxes.contains(Xsource3Axis) =>
          (
            projectVirtualAxes
              .collectFirst { case x: VirtualAxis.ScalaVersionAxis => x }
              .get
              .value,
            project
          )
      }
      .toMap

    val fullMatch = projects.get(scalaVersion)

    def binaryMatch = {
      val scalaBinaryVersion = CrossVersion.binaryScalaVersion(scalaVersion)
      projects.find(_._1.startsWith(scalaBinaryVersion)).map(_._2)
    }

    fullMatch.orElse(binaryMatch).get
  }
}
