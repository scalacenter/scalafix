import Dependencies._
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._
import tut.TutPlugin.autoImport._
import microsites.MicrositesPlugin.autoImport._
import sbtunidoc.BaseUnidocPlugin.autoImport._
import sbtunidoc.ScalaUnidocPlugin.autoImport._
import com.typesafe.sbt.site.SitePlugin.autoImport._
import microsites.ConfigYml
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import com.typesafe.sbt.sbtghpages.GhpagesKeys
import sbt.Def
import sbt.plugins.IvyPlugin

object ScalafixBuild extends AutoPlugin with GhpagesKeys {
  override def trigger = allRequirements
  override def requires = JvmPlugin && IvyPlugin
  object autoImport {
    lazy val stableVersion =
      settingKey[String]("Version of latest release to Maven.")
    lazy val noMima = Seq(
      mimaReportBinaryIssues := {},
      mimaPreviousArtifacts := Set.empty
    )
    lazy val noPublish = Seq(
      skip in publish := true
    ) ++ noMima
    lazy val supportedScalaVersions = List(scala213, scala211, scala212)
    lazy val isFullCrossVersion = Seq(
      crossVersion := CrossVersion.full
    )
    lazy val isScala213 = Def.setting { scalaVersion.value.startsWith("2.13") }
    lazy val warnUnusedImports = Def.setting {
      if (isScala213.value) "-Wunused:imports"
      else "-Ywarn-unused-import"
    }
    lazy val scaladocOptions = Seq(
      "-groups",
      "-implicits"
    )
    lazy val compilerOptions = Def.setting(
      Seq(
        "-target:jvm-1.8",
        warnUnusedImports.value,
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked"
      )
    )

    lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
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

    lazy val testsInputOutputSetting = Seq(
      libraryDependencies ++= testsDeps
    )

    lazy val semanticdbSettings = Seq(
      scalacOptions ++= List(
        "-Yrangepos",
        "-Xplugin-require:semanticdb",
        "-P:semanticdb:synthetics:on"
      ),
      addCompilerPlugin(
        "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
      )
    )

    // =======
    // Website
    // =======
    lazy val docsMappingsAPIDir = settingKey[String](
      "Name of subdirectory in site target directory for api docs"
    )
    lazy val unidocSettings = Seq(
      autoAPIMappings := true,
      apiURL := Some(url("https://scalacenter.github.io/docs/api/")),
      docsMappingsAPIDir := "docs/api",
      addMappingsToSiteDir(
        mappings in (ScalaUnidoc, packageDoc),
        docsMappingsAPIDir
      ),
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-doc-source-url",
        scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
        "-sourcepath",
        baseDirectory.in(LocalRootProject).value.getAbsolutePath,
        "-skip-packages",
        "ammonite:org:scala:scalafix.tests:scalafix.internal"
      ),
      fork in (ScalaUnidoc, unidoc) := true
    )

    lazy val websiteSettings = Seq(
      micrositeName := "scalafix",
      micrositeDescription := "Rewrite and linting tool for Scala",
      micrositeBaseUrl := "scalafix",
      micrositeDocumentationUrl := "docs/users/installation",
      micrositeHighlightTheme := "atom-one-light",
      micrositeHomepage := "https://scalacenter.github.io/scalafix/",
      micrositeOrganizationHomepage := "https://scala.epfl.ch/",
      micrositeTwitterCreator := "@scala_lang",
      micrositeGithubOwner := "scalacenter",
      micrositeGithubRepo := "scalafix",
      ghpagesNoJekyll := false,
      micrositeGitterChannel := true,
      micrositeFooterText := None,
      micrositeFooterText := Some(
        """
          |<p>© 2017 <a href="https://github.com/scalacenter/scalafix#team">The Scalafix Maintainers</a></p>
          |<p style="font-size: 80%; margin-top: 10px">Website built with <a href="https://47deg.github.io/sbt-microsites/">sbt-microsites © 2016 47 Degrees</a></p>
          |""".stripMargin
      ),
      micrositePalette := Map(
        "brand-primary" -> "#0D2B35",
        "brand-secondary" -> "#203F4A",
        "brand-tertiary" -> "#0D2B35",
        "gray-dark" -> "#453E46",
        "gray" -> "rgba(0,0,0,.8)",
        "gray-light" -> "#E3E2E3",
        "gray-lighter" -> "#F4F3F4",
        "white-color" -> "#FFFFFF"
      ),
      micrositeConfigYaml := ConfigYml(
        yamlCustomProperties = Map(
          "githubOwner" -> micrositeGithubOwner.value,
          "githubRepo" -> micrositeGithubRepo.value,
          "docsUrl" -> "docs",
          "callToActionText" -> "Get started",
          "callToActionUrl" -> micrositeDocumentationUrl.value,
          "scala212" -> scala212,
          "scala211" -> scala211,
          "stableVersion" -> stableVersion.value,
          "scalametaVersion" -> scalametaV,
          "supportedScalaVersions" -> supportedScalaVersions,
          "coursierVersion" -> coursierV
        )
      ),
      fork in tut := true
    )
  }
  import autoImport._

  // Custom settings to publish scalafix forks to alternative maven repo.
  lazy val adhocRepoUri = "http://127.0.0.1:8081"
  lazy val adhocRepoCredentials = sys.props("scalafix.repository.credentials")
  lazy val isCustomRepository = adhocRepoUri != null && adhocRepoCredentials != null

  override def globalSettings: Seq[Def.Setting[_]] = List(
    stableVersion := version.in(ThisBuild).value.replaceFirst("\\+.*", ""),
    libraryDependencies ++= List(
      scalacheck % Test,
      scalatest % Test
    ),
    resolvers ++= List(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("public"),
      Resolver.mavenLocal
    ),
    testOptions in Test += Tests.Argument("-oD"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
    commands += Command.command("save-expect") { s =>
      "unit/test:runMain scalafix.tests.util.SaveExpect" ::
        s
    },
    commands += Command.command("ci-213") { s =>
      s"++$scala213" ::
        "unit/test" ::
        "docs/run" ::
        "interfaces/doc" ::
        s
    },
    commands += Command.command("ci-212") { s =>
      s"++$scala212" ::
        "unit/test" ::
        s
    },
    commands += Command.command("ci-211") { s =>
      s"++$scala211" ::
        "unit/test" ::
        s
    },
    commands += Command.command("ci-213-windows") { s =>
      s"++$scala213" ::
        s"unit/testOnly -- -l scalafix.internal.tests.utils.SkipWindows" ::
        s
    },
    commands += Command.command("ci-213-windows") { s =>
      s"++$scala213" ::
        s"unit/testOnly -- -l scalafix.internal.tests.utils.SkipWindows" ::
        s
    },
    commands += Command.command("mima") { s =>
      // Disabled until v0.6.0 stable
      // "scalafix/mimaReportBinaryIssues" ::
      //   "scalafix211/mimaReportBinaryIssues" ::
      s
    },
    // There is flakyness in CliGitDiffTests and CliSemanticTests
    parallelExecution.in(Test) := false,
    publishArtifact.in(Test) := false,
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalacenter/scalafix")),
    autoAPIMappings := true,
    apiURL := Some(url("https://scalacenter.github.io/scalafix/")),
    organization := "ch.epfl.scala",
    developers ++= List(
      Developer(
        "xeno-by",
        "Eugene Burmako",
        "eugene.burmako@gmail.com",
        url("http://xeno.by")
      ),
      Developer(
        "gabro",
        "Gabriele Petronella",
        "gabriele@buildo.io",
        url("https://buildo.io")
      ),
      Developer(
        "MasseGuillaume",
        "Guillaume Massé",
        "masgui@gmail.com",
        url("https://github.com/MasseGuillaume")
      ),
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      ),
      Developer(
        "marcelocenerine",
        "Marcelo Cenerino",
        "marcelocenerine@gmail.com",
        url("https://github.com/marcelocenerine")
      ),
      Developer(
        "ShaneDelmore",
        "Shane Delmore",
        "shane@delmore.io",
        url("https://github.com/shanedelmore")
      )
    )
  )

  private val PreviousScalaVersion = Map(
    "2.11.12" -> "2.11.11",
    "2.12.4" -> "2.12.3",
    "2.12.8" -> "2.12.7",
    "2.12.10" -> "2.12.8"
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    scalacOptions ++= compilerOptions.value,
    scalacOptions.in(Compile, console) :=
      compilerOptions.value :+ "-Yrepl-class-based",
    scalacOptions.in(Compile, doc) ++= scaladocOptions,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalacenter/scalafix"),
        "scm:git:git@github.com:scalacenter/scalafix.git"
      )
    ),
    mimaPreviousArtifacts := {
      val previousArtifactVersion = "0.9.6"
      // NOTE(olafur) shudder, can't figure out simpler way to do the same.
      val binaryVersion =
        if (crossVersion.value.isInstanceOf[CrossVersion.Full]) {
          val version = scalaVersion.value
          PreviousScalaVersion.getOrElse(version, version)
        } else scalaBinaryVersion.value
      Set(
        organization.value % s"${moduleName.value}_$binaryVersion" % previousArtifactVersion
      )
    },
    mimaBinaryIssueFilters ++= Mima.ignoredABIProblems
  )
}
