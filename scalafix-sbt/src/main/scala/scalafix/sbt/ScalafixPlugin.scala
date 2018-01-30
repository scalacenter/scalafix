package scalafix.sbt

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
    val scalafix: InputKey[Unit] =
      inputKey[Unit]("Run scalafix rule.")
    val scalafixCli: InputKey[Unit] =
      inputKey[Unit]("Run scalafix rule.")
    val scalafixTest: InputKey[Unit] =
      inputKey[Unit]("Run scalafix as a test(without modifying sources).")
    val sbtfix: InputKey[Unit] =
      inputKey[Unit](
        "Run scalafix rule on build sources. Requires the semanticdb-sbt plugin to be enabled globally.")
    val sbtfixTest: InputKey[Unit] =
      inputKey[Unit](
        "Run scalafix rule on build sources as a test(without modifying sources).")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
    val scalafixSourceroot: SettingKey[File] = settingKey[File](
      s"Which sourceroot should be used for .semanticdb files.")
    val scalafixVersion: SettingKey[String] = settingKey[String](
      s"Which scalafix version to run. Default is ${Versions.version}.")
    val scalafixScalaVersion: SettingKey[String] = settingKey[String](
      s"Which scala version to run scalafix from. Default is ${Versions.scala212}.")
    val scalafixSemanticdbVersion: SettingKey[String] = settingKey[String](
      s"Which version of semanticdb to use. Default is ${Versions.scalameta}.")
    val scalafixVerbose: SettingKey[Boolean] =
      settingKey[Boolean]("pass --verbose to scalafix")

    def scalafixConfigure(configs: Configuration*): Seq[Setting[_]] =
      List(
        configureForConfigurations(
          configs,
          scalafix,
          c =>
            scalafixTaskImpl(
              c,
              scalafixParserCompat,
              compat = true,
              extraOptions = Nil)),
        configureForConfigurations(
          configs,
          scalafixCli,
          c =>
            scalafixTaskImpl(
              c,
              scalafixParser,
              compat = false,
              extraOptions = Nil)),
        configureForConfigurations(
          configs,
          scalafixTest,
          c =>
            scalafixTaskImpl(
              c,
              scalafixParserCompat,
              compat = true,
              extraOptions = Seq("--test")))
      ).flatten

    /** Add -Yrangepos and semanticdb sourceroot to scalacOptions. */
    def scalafixScalacOptions: Def.Initialize[Seq[String]] =
      ScalafixPlugin.scalafixScalacOptions

    /** Add semanticdb-scalac compiler plugin to libraryDependencies. */
    def scalafixLibraryDependencies: Def.Initialize[List[ModuleID]] =
      ScalafixPlugin.scalafixLibraryDependencies

    /** Enable semanticdb-sbt for all projects with id *-build. */
    def sbtfixSettings: Seq[Def.Setting[_]] =
      ScalafixPlugin.sbtfixSettings

    /** Settings that must appear after scalacOptions and libraryDependencies */
    def scalafixSettings: Seq[Def.Setting[_]] = List(
      scalacOptions ++= scalafixScalacOptions.value,
      libraryDependencies ++= scalafixLibraryDependencies.value
    )

    // TODO(olafur) remove this in 0.6.0, replaced
    val scalafixEnabled: SettingKey[Boolean] =
      settingKey[Boolean](
        "No longer used. Use the scalafixEnable command or manually configure " +
          "scalacOptions/libraryDependecies/scalaVersion")
    @deprecated("Renamed to scalafixSourceroot", "0.5.0")
    val scalametaSourceroot: SettingKey[File] = scalafixSourceroot
  }
  import scalafix.internal.sbt.CliWrapperPlugin.autoImport._
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    scalafixSettings ++ // TODO(olafur) remove this line in 0.6.0
      scalafixTaskSettings ++
      scalafixCliTaskSettings ++
      scalafixTestTaskSettings
  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixEnabled := true,
    scalafixVerbose := false,
    commands += ScalafixEnable.command,
    sbtfix := sbtfixImpl(compat = true).evaluated,
    sbtfixTest := sbtfixImpl(compat = true, extraOptions = Seq("--test")).evaluated,
    aggregate.in(sbtfix) := false,
    aggregate.in(sbtfixTest) := false,
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

  private def sbtfixImpl(compat: Boolean, extraOptions: Seq[String] = Seq()) = {
    Def.inputTaskDyn {
      val baseDir = baseDirectory.in(ThisBuild).value
      val sbtDir: File = baseDir./("project")
      val sbtFiles = baseDir.*("*.sbt").get
      val options =
        "--no-strict-semanticdb" ::
          "--classpath-auto-roots" ::
          baseDir./("target").getAbsolutePath ::
          sbtDir.getAbsolutePath ::
          Nil ++ extraOptions
      scalafixTaskImpl(
        scalafixParserCompat.parsed,
        compat,
        options,
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
      if (scalafixEnabled.value && isSupportedScalaVersion.value) {
        compilerPlugin(
          "org.scalameta" % "semanticdb-scalac" % scalafixSemanticdbVersion.value cross CrossVersion.full
        ) :: Nil
      } else Nil
    }
  lazy val scalafixScalacOptions: Def.Initialize[Seq[String]] = Def.setting {
    if (scalafixEnabled.value && isSupportedScalaVersion.value) {
      Seq(
        "-Yrangepos",
        s"-Xplugin-require:semanticdb",
        s"-P:semanticdb:sourceroot:${scalafixSourceroot.value.getAbsolutePath}"
      )
    } else Nil
  }

  lazy val sbtfixSettings: Seq[Def.Setting[_]] = Def.settings(
    libraryDependencies ++= {
      val sbthost = "org.scalameta" % "semanticdb-sbt" % Versions.semanticdbSbt cross CrossVersion.full
      val isMetabuild = {
        val p = thisProject.value
        p.id.endsWith("-build") && p.base.getName == "project"
      }
      if (isMetabuild) compilerPlugin(sbthost) :: Nil
      else Nil
    }
  )

  // TODO: remove when we can break binary compat
  lazy val scalafixTaskSettings: Seq[Def.Setting[InputTask[Unit]]] =
    configureForConfigurations(
      List(Compile, Test),
      scalafix,
      c =>
        scalafixTaskImpl(
          c,
          scalafixParserCompat,
          compat = true,
          extraOptions = Nil))

  lazy val scalafixCliTaskSettings: Seq[Def.Setting[InputTask[Unit]]] =
    configureForConfigurations(
      List(Compile, Test),
      scalafixCli,
      c =>
        scalafixTaskImpl(c, scalafixParser, compat = false, extraOptions = Nil))

  lazy val scalafixTestTaskSettings: Seq[Def.Setting[InputTask[Unit]]] =
    configureForConfigurations(
      List(Compile, Test),
      scalafixTest,
      c =>
        scalafixTaskImpl(
          c,
          scalafixParserCompat,
          compat = true,
          extraOptions = Seq("--test")))

  @deprecated(
    "Use configureForConfiguration(List(Compile, Test), ...) instead",
    "0.5.4")
  def configureForCompileAndTest(
      task: InputKey[Unit],
      impl: Seq[Configuration] => Def.Initialize[InputTask[Unit]]
  ): Seq[Def.Setting[InputTask[Unit]]] =
    configureForConfigurations(List(Compile, Test), task, impl)

  /** Configure scalafix/scalafixTest tasks for given configurations */
  def configureForConfigurations(
      configurations: Seq[Configuration],
      task: InputKey[Unit],
      impl: Seq[Configuration] => Def.Initialize[InputTask[Unit]]
  ): Seq[Def.Setting[InputTask[Unit]]] =
    (task := impl(configurations).evaluated) +:
      configurations.map(c => task.in(c) := impl(Seq(c)).evaluated)

  def scalafixTaskImpl(
      config: Seq[Configuration],
      parser: Parser[Seq[String]],
      compat: Boolean,
      extraOptions: Seq[String] = Seq()): Def.Initialize[InputTask[Unit]] =
    Def.inputTaskDyn {
      scalafixTaskImpl(
        parser.parsed,
        compat,
        ScopeFilter(configurations = inConfigurations(config: _*)),
        extraOptions)
    }

  def scalafixTaskImpl(
      inputArgs: Seq[String],
      compat: Boolean,
      filter: ScopeFilter,
      extraOptions: Seq[String]): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      compile.all(filter).value // trigger compilation
      val classpath = classDirectory.all(filter).value.asPath
      val sourcesToFix = for {
        sources <- unmanagedSources.all(filter).value
        source <- sources
        if source.exists()
        if canFix(source)
      } yield source
      val options: Seq[String] = List("--classpath", classpath) ++ extraOptions
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
          // run scalafix rules
          val config =
            scalafixConfig.value
              .map(x => "--config" :: x.getAbsolutePath :: Nil)
              .getOrElse(Nil)

          val inputArgs0 =
            if (compat && inputArgs.nonEmpty) "--rules" +: inputArgs
            else inputArgs

          val sourceroot = scalafixSourceroot.value.getAbsolutePath
          // only fix unmanaged sources, skip code generated files.
          verbose ++
            config ++
            inputArgs0 ++
            baseArgs ++
            options ++
            List(
              "--sourceroot",
              sourceroot
            )
        }
        val finalArgs = args ++ files.map(_.getAbsolutePath)
        val nonBaseArgs = args.filterNot(baseArgs).mkString(" ")
        log.info(s"Running scalafix $nonBaseArgs")
        main.main(finalArgs.toArray)
      }
    }
  }

  private def canFix(file: File): Boolean = {
    val path = file.getPath
    path.endsWith(".scala") ||
    path.endsWith(".sbt")
  }

  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}
