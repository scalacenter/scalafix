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

  private val scalafixStub =
    Project(id = s"scalafix-stub", base = file(s"project/scalafix/stub"))
      .settings(
        description :=
          """Project to host scalafix-cli, which is executed to run rewrites.""".stripMargin,
        publishLocal := {},
        publish := {},
        publishArtifact := false,
        publishMavenStyle := false, // necessary to support intransitive dependencies.
        scalaVersion := Versions.scala212,
//        libraryDependencies := Nil, // remove injected dependencies from random sbt plugins.
        libraryDependencies +=
          "ch.epfl.scala" % "scalafix-cli" % scalafixVersion cross CrossVersion.full
      )

  override def extraProjects: Seq[Project] = Seq(scalafixStub)

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperClasspath := {
      CrossVersion.partialVersion(sbtVersion.value) match {
        case Some((0, x)) if x < 13 =>
          streams.value.log
            .warn("sbt-scalafix requires sbt 0.13.13 or higher.")
        case _ =>
      }
      managedClasspath.in(scalafixStub, Compile).value
    },
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile)
  )

  lazy val scalafixTaskImpl = Def.inputTask {
    val main = cliWrapperMain.in(scalafixStub).value
    val log = streams.value.log
    scalahostCompile.value // trigger compilation
    val classpath = scalahostClasspath.value.asPath
    val inputArgs = Def.spaceDelimited("<rewrite>").parsed
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
        config ++
          rewriteArgs ++
          Seq(
            "--no-sys-exit",
            "-i",
            "--sourceroot",
            sourceroot,
            "--classpath",
            classpath
          )
      }
    if (classpath.nonEmpty) {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11 | 12)) =>
          log.info(s"Running scalafix ${args.mkString(" ")}")
          main.main(args.toArray)
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
  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}

// generic plugin for wrapping any command-line interface as an sbt plugin
object CliWrapperPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  def createSyntheticProject(id: String, base: File): Project =
    Project(id, base).settings(publish := {},
                               publishLocal := {},
                               publishArtifact := false)
  class HasMain(reflectiveMain: Main) {
    def main(args: Array[String]): Unit = reflectiveMain.main(args)
  }
  type Main = {
    def main(args: Array[String]): Unit
  }
  object autoImport {
    val cliWrapperClasspath =
      taskKey[Classpath]("classpath to run code generation in")
    val cliWrapperMainClass =
      taskKey[String]("Fully qualified name of main class")
    val cliWrapperMain =
      taskKey[HasMain]("Classloaded instance of main")
  }
  import autoImport._
  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperMain := {
      val cp = cliWrapperClasspath.value.map(_.data.toURI.toURL)
      val cl = new java.net.URLClassLoader(cp.toArray, null)
      val cls = cl.loadClass(cliWrapperMainClass.value)
      val constuctor = cls.getDeclaredConstructor()
      constuctor.setAccessible(true)
      val main = constuctor.newInstance().asInstanceOf[Main]
      new HasMain(main)
    }
  )
}
