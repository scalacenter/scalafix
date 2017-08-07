package scalafix.sbt

import scala.language.reflectiveCalls

import scala.meta.scalahost.sbt.ScalahostSbtPlugin
import scalafix.Versions
import sbt.File
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scalafix.internal.sbt.ScalafixCompletions
import scalafix.internal.sbt.ScalafixJarFetcher
import sbt.Def
import sbt.Def
import sbt.Def
import sbt.complete.DefaultParsers

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  object autoImport {
    val scalafix: InputKey[Unit] = inputKey[Unit]("Run scalafix rewrite.")
    val scalafixVersion: SettingKey[String] = settingKey[String](
      s"Which scalafix version to run. Default is ${Versions.version}.")
    val scalafixScalaVersion: SettingKey[String] = settingKey[String](
      s"Which scala version to run scalafix from. Default is ${Versions.scala212}.")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
  }
  import ScalahostSbtPlugin.autoImport._
  import scalafix.internal.sbt.CliWrapperPlugin.autoImport._
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixVersion := Versions.version,
    scalafixScalaVersion := Versions.scala212,
    cliWrapperClasspath := ScalafixJarFetcher.fetchJars(
      "ch.epfl.scala",
      s"scalafix-cli_${scalafixScalaVersion.value}",
      scalafixVersion.value
    )
  )

  // hack to avoid illegal dynamic reference, can't figure out how to use inputTaskDyn.
  private val workingDirectory = file(sys.props("user.dir"))
  val p = ScalafixCompletions.parser(workingDirectory)

  lazy val scalafixSettings = Seq(
    scalafix.in(Compile) := scalafixTaskImpl(Compile).evaluated,
    scalafix.in(Test) := scalafixTaskImpl(Test).evaluated,
    scalafix := scalafixTaskImpl(Compile, Test).evaluated
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    scalafixSettings

  def scalafixTaskImpl(
      config: Configuration*): Def.Initialize[InputTask[Unit]] =
    Def.inputTaskDyn(scalafixTaskImpl(p.parsed, config: _*))
  def scalafixTaskImpl(
      inputArgs: Seq[String],
      config: Configuration*): Def.Initialize[Task[Unit]] =
    scalafixTaskImpl(
      inputArgs,
      ScopeFilter(configurations = inConfigurations(config: _*)))
  def scalafixTaskImpl(
      inputArgs: Seq[String],
      filter: ScopeFilter): Def.Initialize[Task[Unit]] =
    Def.task {
      val main = cliWrapperMain.value
      val log = streams.value.log

      compile.all(filter).value // trigger compilation
      val classpath = classDirectory.all(filter).value.asPath
      val directoriesToFix: Seq[String] =
        unmanagedSourceDirectories.all(filter).value.flatten.collect {
          case p if p.exists() => p.getAbsolutePath
        }
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
        val sourceroot: String =
          scalametaSourceroot.??(workingDirectory).value.getAbsolutePath
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

  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}
