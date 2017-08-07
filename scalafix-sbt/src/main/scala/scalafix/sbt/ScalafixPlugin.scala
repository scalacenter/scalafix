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

  lazy val scalafixSettings = Seq(
    scalafix := scalafixTaskImpl.evaluated
  )
  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(scalafixSettings) ++
      inConfig(Test)(scalafixSettings) ++ List(
      scalafix := {
        scalafix.in(Compile).evaluated
        scalafix.in(Test).evaluated
      }
    )

  lazy val scalafixTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val main = cliWrapperMain.value
    val log = streams.value.log
    compile.value // trigger compilation
    val classpath = classDirectory.value.getAbsolutePath
    val inputArgs: Seq[String] =
      ScalafixCompletions.parser(state.value.baseDir).parsed
    val directoriesToFix: Seq[String] =
      unmanagedSourceDirectories.value.collect {
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
        scalametaSourceroot
          .??(file(sys.props("user.dir")))
          .value
          .getAbsolutePath
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
