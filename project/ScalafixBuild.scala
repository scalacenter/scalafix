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
import scala.util.Properties
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

    lazy val cliScalaVersions: Seq[String] = {
      val unsupportedVersions: Set[String] =
        if (Properties.isJavaAtLeast("25")) Set(scala212, scala35, scala36)
        else if (!Properties.isJavaAtLeast("17")) Set(scala38)
        else Set.empty
      (scala2Versions ++ scala3Versions).filterNot(unsupportedVersions)
    }
    lazy val cliScalaVersionsWithTargets: Seq[(String, TargetAxis)] =
      cliScalaVersions.map(sv => (sv, TargetAxis(sv))) ++
        cliScalaVersions.intersect(scala2Versions).flatMap { sv =>
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
          }

          val prevVersions = previousVersions(sv).map(prev => TargetAxis(prev))
          val scala3FromScala2 = TargetAxis(scala3Next)
          val xsource3 = TargetAxis(sv, xsource3 = true)

          (prevVersions :+ xsource3).map((sv, _))
        } ++ cliScalaVersions.intersect(Seq(scala3Next)).flatMap { sv =>
          Seq(
            (sv, TargetAxis(scala213)),
            (sv, TargetAxis(scala3LTS))
          )
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
      val minor = scalaVersion.value.split('.')(1).toInt
      if (isScala3.value && minor >= 7)
        Seq("-Wunused:all")
      else if (isScala3.value)
        Seq(
          "-Wunused:all",
          "-Wunused:unsafe-warn-patvars"
        )
      else if (isScala213.value) Seq("-Wunused")
      else Seq("-Ywarn-unused")
    }
    lazy val targetJvm = Def.setting {
      val minor = scalaVersion.value.split('.')(1).toInt
      if (isScala3.value && minor >= 8) Seq("-release:17")
      else if (isScala3.value) Seq("-release:8")
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
            orgScalaLang % "scala-reflect" % scalaVersion.value
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

    def settingsForSemanticdbScalac: Seq[Def.Setting[?]] = Def.settings(
      libraryDependencies ++= {
        if (isScala3.value)
          Seq(
            // CrossVersion.for3Use2_13 would only lookup a binary version artifact, but this is published with full version
            semanticdbScalacCore
              .cross(CrossVersion.constant(scala213))
              .exclude213(semanticdbShared),
            orgScalaLang %% "scala3-presentation-compiler" % scalaVersion.value,
            orgScalaLang %% "scala3-compiler" % scalaVersion.value
          )
        else
          Seq(
            semanticdbScalacCore,
            orgScalaLang % "scala-compiler" % scalaVersion.value,
            orgScalaLang % "scala-reflect" % scalaVersion.value
          )
      }
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
        "supportedScalaVersions" -> cliScalaVersions,
        "scala212" -> scala212,
        "scala213" -> scala213,
        "scala33" -> scala33,
        "scala35" -> scala35,
        "scala36" -> scala36,
        "scala37" -> scala37,
        "scala38" -> scala38,
        "scala3LTS" -> scala3LTS,
        "scala3Next" -> scala3Next,
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
    Test / testOptions += Tests.Argument("-oD"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger,
    commands += Command.command("save-expect") { state =>
      cliScalaVersions
        .map { sv =>
          s"integration${asProjectSuffix(sv)} / Test / runMain scalafix.tests.util.SaveExpect"
        }
        .foldLeft(state) { (state, command) =>
          command :: state
        }
    },
    commands += Command.command("ci-docs") { state =>
      "docs2_13/run" :: // reduce risk of errors on deploy-website.yml
        "interfaces/doc" ::
        state
    },
    commands += Command.command("dogfoodScalafixInterfaces") { state =>
      val extracted = Project.extract(state)
      val v =
        (ThisBuild / version)
          .get(extracted.structure.data)
          .get

      cliScalaVersions
        .map(sv => s"cli${asProjectSuffix(sv)} / publishLocalTransitive")
        .mkString("all ", " ", " interfaces / publishLocal") ::
        "reload plugins" ::
        s"""set dependencyOverrides += "$orgScalafix" % "scalafix-interfaces" % "$v"""" :: // as documented in installation.md
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
    organization := orgScalafix,
    developers ++= Developers.list
  )

  private val PreviousScalaVersion: Map[String, Option[String]] = Map(
    "3.8.3" -> Some("3.8.2")
  )

  override def buildSettings: Seq[Setting[_]] = List(
    // https://github.com/sbt/sbt/issues/5568#issuecomment-1094380636
    versionPolicyIgnored ++=
      runtimeDepsForBackwardCompatibility.map(m => m.organization %% m.name),
    versionPolicyIgnoredInternalDependencyVersions :=
      Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r),
    versionScheme := Some("early-semver"),
    libraryDependencySchemes ++= Seq(
      // Scala 3 compiler
      orgScalaLangMod % "scala-asm" % VersionScheme.Always,
      // coursier-versions always return false for the *.*.*.*-r pattern jgit uses
      Dependencies.jgit.withRevision(VersionScheme.Always)
    )
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    // Prevent issues with scalatest serialization
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    Test / testWindows := (Test / testOnly)
      .toTask(" -- -l scalafix.internal.tests.utils.SkipWindows")
      .value,
    // avoid "missing dependency" on artifacts with full scala version when bumping scala
    versionPolicyIgnored ++= {
      PreviousScalaVersion.values.flatten.toSeq.flatMap { previous =>
        val suffix = "_" + previous
        Seq(
          orgScalameta % (semanticdbScalacCore.name + suffix),
          orgScalafix % ("scalafix-cli" + suffix),
          orgScalafix % ("scalafix-reflect" + suffix),
          orgScalafix % ("scalafix-rules" + suffix)
        )
      }
    },
    versionPolicyIntention := Compatibility.BinaryCompatible,
    scalacOptions ++= werrorOptions,
    scalacOptions ++= Seq( // exclusions
      "-Wconf:cat=deprecation&msg=Rule:s",
      "-Wconf:cat=deprecation&msg=JavaConverters:s",
      "-Wconf:cat=deprecation&msg=AssociatedComments:s"
    ),
    scalacOptions += "-Wconf:origin=scala.collection.compat.*:s",
    scalacOptions ++= compilerOptions.value,
    scalacOptions ++= {
      if (isScala3.value)
        Seq(
          "-Wconf:msg=is no longer supported for vararg splices:s",
          "-Wconf:msg=@nowarn annotation does not suppress any warnings:s"
        )
      else
        Seq(
          "-Wconf:cat=unused-nowarn:s"
        )
    },
    scalacOptions ++= semanticdbSyntheticsCompilerOption.value,
    Compile / console / scalacOptions :=
      compilerOptions.value :+ "-Yrepl-class-based",
    Compile / doc / scalacOptions ++= scaladocOptions,
    Compile / unmanagedSourceDirectories ++= {
      val dir = (Compile / sourceDirectory).value
      scalaVersion.value match {
        case `scala3LTS` => Seq(dir / "scala-3lts")
        case `scala3Next` => Seq(dir / "scala-3next")
        case _ => Nil
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
      val maybePreviousScalaFullV =
        PreviousScalaVersion.get(currentScalaFullV) match {
          case Some(Some(previous)) => Some(previous)
          case None => Some(currentScalaFullV)
          case _ => None
        }

      maybePreviousScalaFullV.fold(Set.empty[ModuleID]) { previousScalaFullV =>
        val previousScalaVCrossName = CrossVersion(
          crossVersion.value,
          previousScalaFullV,
          scalaBinaryVersion.value
        ).getOrElse(identity[String] _)(moduleName.value)
        Set(
          organizationName.value % previousScalaVCrossName % stableVersion.value
        )
      }
    },
    mimaBinaryIssueFilters ++= Mima.ignoredABIProblems
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

  private def asProjectSuffix(scalaVersion: String) =
    scalaVersion.replaceAll("[\\.-]", "_")

  val werrorOptions = Seq(
    "-Werror",
    "-deprecation",
    "-Wconf:cat=deprecation:error"
  )

}
