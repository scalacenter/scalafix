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

object ScalafixBuild extends AutoPlugin with GhpagesKeys {
  override def trigger = allRequirements
  override def requires = JvmPlugin
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
    lazy val supportedScalaVersions = List(scala211, scala212)
    lazy val isFullCrossVersion = Seq(
      crossVersion := CrossVersion.full
    )
    lazy val warnUnusedImports = "-Ywarn-unused-import"
    lazy val compilerOptions = Seq(
      "-target:jvm-1.8",
      warnUnusedImports,
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked"
    )

    lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
      buildInfoKeys := Seq[BuildInfoKey](
        name,
        version,
        stableVersion,
        "coursier" -> coursier.util.Properties.version,
        "nightly" -> version.value,
        "scalameta" -> scalametaV,
        scalaVersion,
        "supportedScalaVersions" -> supportedScalaVersions,
        "scala211" -> scala211,
        "scala212" -> scala212,
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
          "coursierVersion" -> coursier.util.Properties.version
        )
      ),
      fork in tut := true
    )
  }
  import autoImport._

  // Custom settings to publish scalafix forks to alternative maven repo.
  lazy val adhocRepoUri = sys.props("scalafix.repository.uri")
  lazy val adhocRepoCredentials = sys.props("scalafix.repository.credentials")
  lazy val isCustomRepository = adhocRepoUri != null && adhocRepoCredentials != null

  override def globalSettings: Seq[Def.Setting[_]] = List(
    stableVersion := "0.5.10", // hardcoded while we iterate through v0.6 milestones.
    scalacOptions ++= compilerOptions,
    scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
    libraryDependencies ++= List(
      scalacheck % Test,
      scalatest % Test
    ),
    resolvers ++= List(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      Resolver.mavenLocal
    ),
    testOptions in Test += Tests.Argument("-oD"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
    commands += Command.command("ci-212") { s =>
      "++2.12.6" ::
        "unit/test" ::
        "docs/run" ::
        "interfaces/doc" ::
        s
    },
    commands += Command.command("ci-211") { s =>
      "++2.11.12" ::
        "unit/test" ::
        s
    },
    commands += Command.command("ci-212-windows") { s =>
      "++2.12.6" ::
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
    credentials ++= {
      val credentialsFile = {
        if (adhocRepoCredentials != null) new File(adhocRepoCredentials)
        else null
      }
      if (credentialsFile != null) List(new FileCredentials(credentialsFile))
      else Nil
    },
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
    "2.12.4" -> "2.12.3"
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := Some {
      if (isCustomRepository) "adhoc" at adhocRepoUri
      else if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    },
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalacenter/scalafix"),
        "scm:git:git@github.com:scalacenter/scalafix.git"
      )
    ),
    mimaPreviousArtifacts := {
      val previousArtifactVersion = "0.5.0"
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
