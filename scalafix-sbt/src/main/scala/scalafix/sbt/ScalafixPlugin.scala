package scalafix.sbt

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

trait ScalafixKeys {
  val scalafixCompile: TaskKey[Unit] =
    taskKey[Unit](
      "Fix Scala sources using scalafix. Note, this task runs clean.")

  val scalafixConfigurations: SettingKey[Seq[Configuration]] =
    settingKey[Seq[Configuration]](
      "Which configurations should scalafix run in?" +
        " Defaults to Compile and Test.")
}

object ScalafixPlugin extends AutoPlugin with ScalafixKeys {
  override def requires = JvmPlugin
  override def trigger: PluginTrigger = AllRequirements
  val nightlyVersion: String = _root_.scalafix.Versions.nightly
  val scalafix: Command = Command.command(
    name = "scalafix",
    briefHelp = "Run scalafix rewrite rules. Note, will clean the build.",
    detail =
      "Injects the scalafix compiler plugin into your sbt session " +
        "and then clean compiles all source files. " +
        "Scalafix rewrite rules are executed from within the compiler " +
        "plugin during compilation. " +
        "Once scalafix has completed, the session is clear."
  ) { state =>
    // TODO(olafur) Is there a cleaner way to accomplish the same?
    // Requirements: the plugin must be enabled only during the scalafix task/command.
    val addScalafixCompilerPluginSetting: String =
      s"""libraryDependencies in ThisBuild +=
         |  compilerPlugin("ch.epfl.scala" %% "scalafix-nsc" % "$nightlyVersion")""".stripMargin
    s"set $addScalafixCompilerPluginSetting" ::
      "show scalacOptions" ::
        "scalafixCompile" ::
          s"session clear" ::
            state
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      commands ++= Seq(scalafix),
      scalafixConfigurations := Seq(Compile, Test),
      scalafixCompile := Def.taskDyn {
        val cleanCompileInAllConfigurations =
          scalafixConfigurations.value.map(x =>
            (compile in x).dependsOn(clean in x))
        Def
          .task(())
          .dependsOn(cleanCompileInAllConfigurations: _*)
      }.value
    )

  object autoImport extends ScalafixKeys

}
