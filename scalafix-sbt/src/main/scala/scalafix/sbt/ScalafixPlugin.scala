package scalafix.sbt

import scala.language.reflectiveCalls

import scalafix.Versions
import sbt.File
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scalafix.internal.sbt.ScalafixCompletions
import scalafix.internal.sbt.ScalafixJarFetcher
import sbt.Def
import sbt.Def

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  object autoImport {
    val scalafix: InputKey[Unit] = inputKey[Unit]("Run scalafix rewrite.")
    val scalafixBuild: InputKey[Unit] = inputKey[Unit](
      "Run scalafix rewrite on build sources. " +
        "Requires the sbthost plugin to be enabled globally.")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
    val scalafixEnabled: SettingKey[Boolean] =
      settingKey[Boolean](
        "If false, scalafix will not enable the semanticdb-scalac " +
          "compiler plugin, which is necessary for semantic rewrites.")
    def scalafixScalacOptions: Def.Initialize[Seq[String]] =
      ScalafixPlugin.scalafixScalacOptions
    def scalafixBuildSettings: Seq[Def.Setting[_]] =
      ScalafixPlugin.scalafixBuildSettings
    val scalafixVerbose: SettingKey[Boolean] =
      settingKey[Boolean]("pass --verbose to scalafix")
    def scalafixSettings: Seq[Def.Setting[_]] =
      scalafixTaskSettings ++
        scalafixScalacSettings
    val scalafixSourceroot: SettingKey[File] = settingKey[File](
      s"Which sourceroot should be used for .semanticdb files.")
    @deprecated("Renamed to scalafixSourceroot", "0.5.0")
    val scalametaSourceroot: SettingKey[File] = scalafixSourceroot
    val scalafixVersion: SettingKey[String] = settingKey[String](
      s"Which scalafix version to run. Default is ${Versions.version}.")
    val scalafixScalaVersion: SettingKey[String] = settingKey[String](
      s"Which scala version to run scalafix from. Default is ${Versions.scala212}.")
    val scalafixSemanticdbVersion: SettingKey[String] = settingKey[String](
      s"Which version of semanticdb to sue. Default is ${Versions.scalameta}.")
  }
  import scalafix.internal.sbt.CliWrapperPlugin.autoImport._
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = scalafixSettings
  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixEnabled := true,
    scalafixVerbose := false,
    scalafixBuild := Def.inputTaskDyn {
      val baseDir = (baseDirectory in ThisBuild).value
      val sbtDir: File = baseDir./("project")
      val sbtFiles = baseDir.*("*.sbt").get
      scalafixTaskImpl(
        scalafixParser.parsed,
        Seq.empty[String],
        sbtDir +: sbtFiles,
        "sbt-build",
        streams.value
      )
    }.evaluated,
    aggregate.in(scalafixBuild) := false,
    scalafixSourceroot := baseDirectory.in(ThisBuild).value,
    scalafixVersion := Versions.version,
    scalafixSemanticdbVersion := Versions.scalameta,
    scalafixScalaVersion := Versions.scala212,
    cliWrapperClasspath := {
      val jars = ScalafixJarFetcher.fetchJars(
        "ch.epfl.scala",
        s"scalafix-cli_${scalafixScalaVersion.value}",
        scalafixVersion.value
      )
      if (jars.isEmpty) {
        throw new MessageOnlyException("Unable to download scalafix-cli jars!")
      }
      jars
    }
  )

  // hack to avoid illegal dynamic reference, can't figure out how to do
  // scalafixParser(baseDirectory.in(ThisBuild).value).parsed
  private def workingDirectory = file(sys.props("user.dir"))
  private val scalafixParser = ScalafixCompletions.parser(workingDirectory)
  private val isSupportedScalaVersion = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => true
      case _ => false
    }
  }
  private object logger {
    def warn(msg: String): Unit = {
      println(
        s"[${scala.Console.YELLOW}warn${scala.Console.RESET}] scalafix - $msg")
    }
  }

  lazy val scalafixScalacOptions: Def.Initialize[Seq[String]] = Def.setting {
    if (!scalafixEnabled.value) Nil
    else {
      Seq(
        "-Yrangepos",
        s"-Xplugin-require:semanticdb",
        s"-P:semanticdb:sourceroot:${scalafixSourceroot.value.getAbsolutePath}"
      )
    }
  }

  lazy val scalafixBuildSettings: Seq[Def.Setting[_]] = Def.settings(
    libraryDependencies ++= {
      val sbthost = "org.scalameta" % "sbthost-nsc" % "0.3.1" cross CrossVersion.full
      val isMetabuild = {
        val p = thisProject.value
        p.id.endsWith("-build") && p.base.getName == "project"
      }
      if (isMetabuild) compilerPlugin(sbthost) :: Nil
      else Nil
    }
  )

  lazy val scalafixScalacSettings: Seq[Def.Setting[_]] = Def.settings(
    scalacOptions ++= {
      if (isSupportedScalaVersion.value) {
        scalafixScalacOptions.value
      } else {
        Nil
      }
    },
    libraryDependencies ++= {
      if (isSupportedScalaVersion.value && scalafixEnabled.value) {
        // Only add compiler plugin in 2.11 and 2.12 projects.
        if (!Versions.supportedScalaVersions.contains(scalaVersion.value)) {
          val supportedVersion =
            Versions.supportedScalaVersions.mkString(", ")
          logger.warn(
            s"Unsupported ${thisProject.value.id}/scalaVersion ${scalaVersion.value}. " +
              s"Please upgrade to one of: $supportedVersion")
        }
        val semanticdb =
          "org.scalameta" %
            "semanticdb-scalac" %
            scalafixSemanticdbVersion.value cross CrossVersion.full
        compilerPlugin(semanticdb) :: Nil
      } else {
        Nil
      }
    }
  )
  lazy val scalafixTaskSettings = Seq(
    scalafix.in(Compile) := scalafixTaskImpl(Compile).evaluated,
    scalafix.in(Test) := scalafixTaskImpl(Test).evaluated,
    scalafix := scalafixTaskImpl(Compile, Test).evaluated
  )

  def scalafixTaskImpl(
      config: Configuration*): Def.Initialize[InputTask[Unit]] =
    Def.inputTaskDyn {
      scalafixTaskImpl(
        scalafixParser.parsed,
        ScopeFilter(configurations = inConfigurations(config: _*)))
    }

  def scalafixTaskImpl(
      inputArgs: Seq[String],
      filter: ScopeFilter): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      compile.all(filter).value // trigger compilation
      val classpath = classDirectory.all(filter).value.asPath
      val directoriesToFix: Seq[File] =
        unmanagedSourceDirectories.all(filter).value.flatten.collect {
          case p if p.exists() => p.getAbsoluteFile
        }
      scalafixTaskImpl(
        inputArgs,
        List("--classpath", classpath),
        directoriesToFix,
        thisProject.value.id,
        streams.value
      )
    }

  def scalafixTaskImpl(
      inputArgs: Seq[String],
      options: Seq[String],
      files: Seq[File],
      projectId: String,
      streams: TaskStreams
  ): Def.Initialize[Task[Unit]] = {
    if (files.isEmpty) Def.task(())
    else {
      Def.task {
        val log = streams.log
        val verbose = if (scalafixVerbose.value) "--verbose" :: Nil else Nil
        val main = cliWrapperMain.value
        val baseArgs = Set[String](
          "--project-id",
          projectId,
          "--no-sys-exit",
          "--non-interactive"
        )
        val args: Seq[String] = {
          // run scalafix rewrites
          val config =
            scalafixConfig.value
              .map(x => "--config" :: x.getAbsolutePath :: Nil)
              .getOrElse(Nil)
          val rewriteArgs =
            if (inputArgs.nonEmpty)
              inputArgs.flatMap("-r" :: _ :: Nil)
            else Nil
          val sourceroot = scalafixSourceroot.value.getAbsolutePath
          // only fix unmanaged sources, skip code generated files.
          verbose ++
            config ++
            rewriteArgs ++
            baseArgs ++
            options ++
            List(
              "--sourceroot",
              sourceroot
            )
        }
        val finalArgs = args ++ files.map(_.getAbsolutePath)
        val nonBaseArgs = finalArgs.filterNot(baseArgs).mkString(" ")
        log.info(s"Running scalafix $nonBaseArgs")
        main.main(finalArgs.toArray)
      }
    }
  }

  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}
