package scalafix.sbt

import scala.language.reflectiveCalls

import scalafix.Versions
import java.io.File
import sbt.File
import sbt.Keys.{version => _}
import sbt.Keys._
import sbt.ScopeFilter.ScopeFilter
import sbt._
import sbt.inc.Analysis
import sbt.plugins.JvmPlugin
import scala.meta.scalahost.sbt.ScalahostSbtPlugin
import scalafix.internal.sbt.CliWrapperPlugin

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins =
    CliWrapperPlugin && ScalahostSbtPlugin && JvmPlugin
  object autoImport {
    val scalafix = inputKey[Unit]("Run scalafix")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
  }
  import CliWrapperPlugin.autoImport._
  import autoImport._
  private val scalafixVersion = _root_.scalafix.Versions.version

  // override injected settings by other sbt plugins.
  private val leaveMeAlone = Seq(
    skip in publish := true,
    publishLocal := {},
    publish := {},
    test := {},
    sources := Nil,
    scalacOptions := Nil,
    javaOptions := Nil,
    libraryDependencies := Nil,
    publishArtifact := false,
    publishMavenStyle := false
  )

  private val scalafixStub =
    Project(id = s"scalafix-stub", base = file(s"project/scalafix/stub"))
      .settings(
        leaveMeAlone,
        description :=
          """Project to fetch jars for ch.epfl.scala:scalafix-cli_2.11, since there
            |is no nice api in sbt to get jars for a ModuleId outside of
            |a project. Please upvote https://github.com/sbt/sbt/issues/2879 to
            |make this hack unnecessary.""".stripMargin,
        scalaVersion := Versions.scala212,
        libraryDependencies := Seq(
          "ch.epfl.scala" % "scalafix-cli" % scalafixVersion cross CrossVersion.full
        )
      )

  override def extraProjects: Seq[Project] = Seq(scalafixStub)

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperClasspath := {
      CrossVersion.partialVersion(sbtVersion.value) match {
        case Some((0, x)) if x < 13 =>
          throw new MessageOnlyException(
            "sbt-scalafix requires sbt 0.13.13 or higher.")
        case _ =>
      }
      managedClasspath.in(scalafixStub, Compile).value
    },
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile)
  )

  lazy val scalafixTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val main = cliWrapperMain.in(scalafixStub).value
    val log = streams.value.log
    scalahostCompile.value // trigger compilation
    val classpath = scalahostClasspath.value.asPath
    val inputArgs = Def.spaceDelimited("<rewrite>").parsed
    val directoriesToFix: Seq[String] =
      scalafixUnmanagedSources.value.flatMap(_.collect {
        case p if p.exists() => p.getAbsolutePath
      })
    val args: Seq[String] =
      if (inputArgs.nonEmpty &&
          inputArgs.exists(_.startsWith("-"))) {
        // run custom command
        inputArgs
      } else {
        // run scalafix rewrites
        val config =
          scalafixConfig.value
            .map(x => "--config" :: x.getAbsolutePath :: Nil)
            .getOrElse(Nil)
        val rewriteArgs =
          if (inputArgs.nonEmpty)
            "--rewrites" +: inputArgs
          else Nil
        val sourceroot =
          ScalahostSbtPlugin.autoImport.scalametaSourceroot.value.getAbsolutePath
        // only fix unmanaged sources, skip code generated files.
        config ++
          rewriteArgs ++
          Seq(
            "--no-sys-exit",
            "--sourceroot",
            sourceroot,
            "--classpath",
            classpath
          )
      }
    if (classpath.nonEmpty) {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11 | 12)) if directoriesToFix.nonEmpty =>
          log.info(s"Running scalafix ${args.mkString(" ")}")
          main.main((args ++ directoriesToFix).toArray)
        case _ => // do nothing
      }
    }
  }
  lazy val scalafixSettings = Seq(
    scalafix := scalafixTaskImpl.evaluated
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(scalafixSettings) ++
      inConfig(Test)(scalafixSettings)

  private def scalahostAggregateFilter: Def.Initialize[ScopeFilter] =
    Def.setting {
      ScopeFilter(configurations = inConfigurations(Compile, Test))
    }
  lazy private val scalahostClasspath: Def.Initialize[Seq[File]] =
    Def.settingDyn(classDirectory.all(scalahostAggregateFilter.value))
  lazy private val scalahostCompile: Def.Initialize[Task[Seq[Analysis]]] =
    Def.taskDyn(compile.all(scalahostAggregateFilter.value))
  lazy private val scalafixUnmanagedSources: Def.Initialize[Seq[Seq[File]]] =
    Def.settingDyn(
      unmanagedSourceDirectories.all(scalahostAggregateFilter.value))
  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}
