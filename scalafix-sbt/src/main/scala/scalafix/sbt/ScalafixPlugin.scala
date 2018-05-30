package scalafix.sbt

import java.util
import scalafix.Versions
import sbt.File
import sbt.Keys._
import sbt._
import sbt.complete.Parser
import sbt.plugins.JvmPlugin
import scalafix.internal.sbt.ScalafixCompletions
import scalafix.internal.sbt.ScalafixJarFetcher
import sbt.Def

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  object autoImport {
    val Scalafix = Tags.Tag("scalafix")

    val scalafix: InputKey[Unit] =
      inputKey[Unit]("Run scalafix rule.")
    val scalafixCli: InputKey[Unit] =
      inputKey[Unit]("Run scalafix rule.")
    val scalafixTest: InputKey[Unit] =
      inputKey[Unit](
        "Runs scalafix failing the build if sources would change. Does not modify files.")
    val scalafixAutoSuppressLinterErrors: InputKey[Unit] =
      inputKey[Unit](
        "Run scalafix and automatically suppress linter errors " +
          "by inserting /* scalafix:ok */ comments into the source code. " +
          "Useful when migrating an existing large codebase with many linter errors.")
    val sbtfix: InputKey[Unit] =
      inputKey[Unit](
        "Run syntactic scalafix rule on build sources. Note, semantic rewrites are not supported.")
    val sbtfixTest: InputKey[Unit] =
      inputKey[Unit](
        "Run scalafix on build sources failing the build if source would change. Does not modify files.")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
    val scalafixVersion: SettingKey[String] = settingKey[String](
      s"Which scalafix version to run. Default is ${Versions.version}.")
    val scalafixScalaVersion: SettingKey[String] = settingKey[String](
      s"Which scala version to run scalafix from. Default is ${Versions.scala212}.")
    val scalafixSemanticdbVersion: SettingKey[String] = settingKey[String](
      s"Which version of semanticdb to use. Default is ${Versions.scalameta}.")
    val scalafixMetacpCacheDirectory: SettingKey[Option[File]] = settingKey(
      "Global cache location to persist metacp artifacts produced by analyzing --dependency-classpath. " +
        "The default location depends on the OS and is computed with https://github.com/soc/directories-jvm " +
        "using the project name 'semanticdb'. " +
        "On macOS the default cache directory is ~/Library/Caches/semanticdb. ")
    val scalafixParallel: SettingKey[Boolean] = settingKey(
      "If false (default), run scalafix in single-threaded mode. " +
        "If true, allow scalafix to utilize available CPUs. " +
        "Default is false because sbt automatically parallelizes across projects and configurations."
    )
    val scalafixSemanticdb =
      "org.scalameta" % "semanticdb-scalac" % Versions.scalameta cross CrossVersion.full
    val scalafixVerbose: SettingKey[Boolean] =
      settingKey[Boolean]("pass --verbose to scalafix")

    lazy val scalafixConfigSettings: Seq[Def.Setting[_]] = Seq(
      scalafix := scalafixTaskImpl(
        scalafixParserCompat,
        compat = true,
        Seq("--format", "sbt")).tag(Scalafix).evaluated,
      scalafixTest := scalafixTaskImpl(
        scalafixParserCompat,
        compat = true,
        Seq("--test", "--format", "sbt")).tag(Scalafix).evaluated,
      scalafixCli := scalafixTaskImpl(
        scalafixParser,
        compat = false,
        Seq("--format", "sbt")).tag(Scalafix).evaluated,
      scalafixAutoSuppressLinterErrors := scalafixTaskImpl(
        scalafixParser,
        compat = true,
        Seq("--auto-suppress-linter-errors", "--format", "sbt"))
        .tag(Scalafix)
        .evaluated
    )

    @deprecated("This setting is no longer used", "0.6.0")
    val scalafixSourceroot: SettingKey[File] = settingKey[File]("Unused")
    @deprecated("Use scalacOptions += -Yrangepos instead", "0.6.0")
    def scalafixScalacOptions: Def.Initialize[Seq[String]] =
      ScalafixPlugin.scalafixScalacOptions
    @deprecated("Use addCompilerPlugin(semanticdb-scalac) instead", "0.6.0")
    def scalafixLibraryDependencies: Def.Initialize[List[ModuleID]] =
      ScalafixPlugin.scalafixLibraryDependencies
    @deprecated("This setting is no longer used", "0.6.0")
    def sbtfixSettings: Seq[Def.Setting[_]] = Nil
    @deprecated(
      "Use addCompilerPlugin(scalafixSemanticdb) and scalacOptions += \"-Yrangepos\" instead",
      "0.6.0")
    def scalafixSettings: Seq[Def.Setting[_]] = List(
      scalacOptions ++= scalafixScalacOptions.value,
      libraryDependencies ++= scalafixLibraryDependencies.value
    )
  }
  import scalafix.internal.sbt.CliWrapperPlugin.autoImport._
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test).flatMap(inConfig(_)(scalafixConfigSettings))

  private val cachedCoursierJars =
    util.Collections.synchronizedMap(
      new util.HashMap[(String, String), Seq[File]])
  private val fetchScalafixJars =
    new util.function.Function[(String, String), Seq[File]] {
      override def apply(t: (String, String)): Seq[File] = {
        val (scalafixScalaVersion, scalafixVersion) = t
        ScalafixJarFetcher.fetchJars(
          "ch.epfl.scala",
          s"scalafix-cli_$scalafixScalaVersion",
          scalafixVersion
        )
      }
    }

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    cliWrapperMainClass := "scalafix.v1.Main$",
    scalafixVerbose := false,
    commands += ScalafixEnable.command,
    sbtfix := sbtfixImpl(compat = true).evaluated,
    sbtfixTest := sbtfixImpl(compat = true, extraOptions = Seq("--test")).evaluated,
    aggregate.in(sbtfix) := false,
    aggregate.in(sbtfixTest) := false,
    scalafixVersion := Versions.version,
    scalafixSemanticdbVersion := Versions.scalameta,
    scalafixScalaVersion := Versions.scala212,
    scalafixMetacpCacheDirectory := None,
    scalafixParallel := false,
    cliWrapperClasspath := {
      val jars = cachedCoursierJars.computeIfAbsent(
        scalafixScalaVersion.value -> scalafixVersion.value,
        fetchScalafixJars
      )
      if (jars.isEmpty) {
        throw new MessageOnlyException("Unable to download scalafix-cli jars!")
      }
      jars
    }
  )

  private def sbtfixImpl(compat: Boolean, extraOptions: Seq[String] = Seq()) = {
    Def.inputTaskDyn {
      val baseDir = baseDirectory.in(ThisBuild).value
      val sbtDir: File = baseDir./("project")
      val sbtFiles = baseDir.*("*.sbt").get
      scalafixTaskImpl(
        scalafixParserCompat.parsed,
        compat,
        extraOptions,
        sbtDir +: sbtFiles,
        "sbt-build",
        streams.value
      )
    }
  }

  // hack to avoid illegal dynamic reference, can't figure out how to do
  // scalafixParser(baseDirectory.in(ThisBuild).value).parsed
  private def workingDirectory = file(sys.props("user.dir"))

  private val scalafixParser =
    ScalafixCompletions.parser(workingDirectory.toPath, compat = false)
  private val scalafixParserCompat =
    ScalafixCompletions.parser(workingDirectory.toPath, compat = true)

  private val isSupportedScalaVersion = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => true
      case _ => false
    }
  }

  lazy val scalafixLibraryDependencies: Def.Initialize[List[ModuleID]] =
    Def.setting {
      if (isSupportedScalaVersion.value) {
        compilerPlugin(
          "org.scalameta" % "semanticdb-scalac" % scalafixSemanticdbVersion.value cross CrossVersion.full
        ) :: Nil
      } else Nil
    }
  lazy val scalafixScalacOptions: Def.Initialize[Seq[String]] = Def.setting {
    if (isSupportedScalaVersion.value) {
      Seq(
        "-Yrangepos",
        s"-Xplugin-require:semanticdb"
      )
    } else Nil
  }

  def scalafixTaskImpl(
      parser: Parser[Seq[String]],
      compat: Boolean,
      extraOptions: Seq[String] = Seq()): Def.Initialize[InputTask[Unit]] =
    Def.inputTaskDyn {
      scalafixTaskImpl(parser.parsed, compat, extraOptions)
    }

  def scalafixTaskImpl(
      inputArgs: Seq[String],
      compat: Boolean,
      extraOptions: Seq[String]): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      compile.value // trigger compilation
      val scalafixClasspath =
        classDirectory.value +:
          dependencyClasspath.value.map(_.data)
      val sourcesToFix = for {
        source <- unmanagedSources.in(scalafix).value
        if source.exists()
        if canFix(source)
      } yield source
      val options: Seq[String] = List(
        "--classpath",
        scalafixClasspath.mkString(java.io.File.pathSeparator)
      ) ++ extraOptions
      scalafixTaskImpl(
        inputArgs,
        compat,
        options,
        sourcesToFix,
        thisProject.value.id,
        streams.value
      )
    }

  def scalafixTaskImpl(
      inputArgs: Seq[String],
      compat: Boolean,
      options: Seq[String],
      files: Seq[File],
      projectId: String,
      streams: TaskStreams
  ): Def.Initialize[Task[Unit]] = {
    if (files.isEmpty) Def.task(())
    else {
      Def.task {
        val args = Array.newBuilder[String]

        if (scalafixVerbose.value) {
          args += "--verbose"
        }

        scalafixMetacpCacheDirectory.value match {
          case Some(dir) =>
            args += (
              "--metacp-cache-dir",
              dir.absolutePath
            )
          case _ =>
        }

        scalafixConfig.value match {
          case Some(x) =>
            args += (
              "--config",
              x.getAbsolutePath
            )
          case _ =>
        }

        if (compat && inputArgs.nonEmpty) {
          args += "--rules"
        }
        args ++= inputArgs
        args ++= options

        val nonBaseArgs = args.result().mkString(" ")
        if (scalafixVerbose.value) {
          streams.log.info(s"Running scalafix $nonBaseArgs")
        } else if (files.lengthCompare(1) > 0) {
          streams.log.info(s"Running scalafix on ${files.size} Scala sources")
        }

        args += "--no-sys-exit"

        args ++= files.iterator.map(_.getAbsolutePath)

        cliWrapperMain.value.main(args.result())
      }
    }
  }

  private def canFix(file: File): Boolean = {
    val path = file.getPath
    path.endsWith(".scala") ||
    path.endsWith(".sbt")
  }
}
