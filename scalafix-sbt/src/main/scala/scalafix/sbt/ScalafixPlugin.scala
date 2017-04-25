package scalafix.sbt

import scala.language.reflectiveCalls

import scala.meta.scalahost.sbt.ScalahostSbtPlugin
import scalafix.Versions

import java.io.File

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

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
  import autoImport._
  import CliWrapperPlugin.autoImport._
  private val scalafixVersion = _root_.scalafix.Versions.version
  private val disabled = sys.props.contains("scalafix.disable")
  private def jar(report: UpdateReport): File =
    report.allFiles
      .find { x =>
        x.getAbsolutePath.matches(
          // publishLocal produces jars with name `VERSION/scalafix-nsc_2.11.jar`
          // while the jars published with publishSigned to Maven are named
          // `scalafix-nsc_2.11-VERSION.jar`
          s".*scalafix-nsc_2.1[12].(\\d+)(-$scalafixVersion)?.jar$$")
      }
      .getOrElse {
        throw new IllegalStateException(
          s"Unable to resolve scalafix-nsc compiler plugin. Report: $report"
        )
      }

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
          "ch.epfl.scala" % "scalafix-fatcli" % scalafixVersion cross CrossVersion.full
      )

  override def extraProjects: Seq[Project] = Seq(scalafixStub)

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperClasspath := managedClasspath.in(scalafixStub, Compile).value,
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile)
  )

  lazy val scalafixTaskImpl = Def.inputTask {
    val main = cliWrapperMain.in(scalafixStub).value
    val config =
      scalafixConfig.value
        .map(x => "--config" :: x.getAbsolutePath :: Nil)
        .getOrElse(Nil)
    val log = streams.value.log
    val sourcepath = scalahostSourcepath.value.flatten
    val classpath = scalahostClasspath.value.flatMap(_.map(_.data))
    def format(files: Seq[File]): String =
      files.map(_.getAbsolutePath).mkString(File.pathSeparator)
    val inputArgs = Def.spaceDelimited("<rewrite>").parsed
    val rewriteArgs =
      if (inputArgs.nonEmpty) "--rewrites" +: inputArgs
      else Nil
    val args =
      config ++
        rewriteArgs ++
        Seq(
          "--no-sys-exit",
          "-i",
          "--sourcepath",
          format(sourcepath),
          "--classpath",
          format(classpath)
        )
    log.info(s"Running scalafix ${args.mkString(" ")}...")
    if (sourcepath.nonEmpty && classpath.nonEmpty) main.main(args.toArray)
  }
  lazy val scalafixSettings = Seq(
    scalafix := scalafixTaskImpl.evaluated
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(scalafixSettings) ++
      inConfig(Test)(scalafixSettings) ++
      inConfig(IntegrationTest)(scalafixSettings)
  // TODO(olafur) remove
  private def scalahostAggregateFilter: Def.Initialize[ScopeFilter] =
    Def.setting {
      ScopeFilter(configurations = inConfigurations(Compile, Test))
    }
  lazy private val scalahostSourcepath: Def.Initialize[Seq[Seq[File]]] =
    Def.settingDyn(sourceDirectories.all(scalahostAggregateFilter.value))
  lazy private val scalahostClasspath: Def.Initialize[Task[Seq[Classpath]]] =
    Def.taskDyn(fullClasspath.all(scalahostAggregateFilter.value))
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
