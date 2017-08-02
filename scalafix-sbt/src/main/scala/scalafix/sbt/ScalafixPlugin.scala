package scalafix.sbt

import scala.meta.scalahost.sbt.ScalahostSbtPlugin
import scalafix.Versions
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scalafix.internal.sbt.ScalafixCompletions

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  object autoImport {
    val Scalafix = config("scalafix")
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
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    scalafixVersion := Versions.version,
    scalafixScalaVersion := Versions.scala212
  )

  lazy val scalafixSettings = Seq(
    scalafix := scalafixTaskImpl.evaluated
  )

  def scalafixRuntimeSettings =
    inConfig(Scalafix)(
      Seq(
        mainClass := Some("scalafix.cli.Cli"),
        fullClasspath := Classpaths
          .managedJars(Scalafix, classpathTypes.value, update.value),
        runner in (Scalafix, run) := {
          val forkOptions = ForkOptions(
            bootJars = Nil,
            javaHome = javaHome.value,
            connectInput = connectInput.value,
            outputStrategy = outputStrategy.value,
            runJVMOptions = javaOptions.value,
            workingDirectory = Some(baseDirectory.value),
            envVars = envVars.value
          )
          new ForkRun(forkOptions)
        }
      ))

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(scalafixSettings) ++
      inConfig(Test)(scalafixSettings) ++
      scalafixRuntimeSettings ++ List(
      ivyConfigurations += Scalafix,
      scalafix := {
        scalafix.in(Compile).evaluated
        scalafix.in(Test).evaluated
      },
      libraryDependencies ++= List(
        // Explicitly set the Scala version specific dependencies so the resolution
        // doesn't pick up any dependencies automatically added by sbt based on the
        // the project the plugin is enabled in.
        "org.scala-lang" % "scala-library" % scalafixScalaVersion.value % Scalafix,
        "org.scala-lang" % "scala-reflect" % scalafixScalaVersion.value % Scalafix,
        "org.scala-lang" % "scala-compiler" % scalafixScalaVersion.value % Scalafix,
        "ch.epfl.scala" % s"scalafix-cli_${scalafixScalaVersion.value}" % scalafixVersion.value % Scalafix
      )
    )

  lazy val scalafixTaskImpl: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val log = streams.value.log
    compile.value // trigger compilation
    val classpath = classDirectory.value.getAbsolutePath
    val inputArgs: Seq[String] = ScalafixCompletions.parser.parsed
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
          (runner in (Scalafix, run)).value.run(
            (mainClass in Scalafix).value.get,
            Attributed.data((fullClasspath in Scalafix).value),
            args ++ directoriesToFix,
            streams.value.log
          )
        case _ => // do nothing
      }
    }
  }
}
