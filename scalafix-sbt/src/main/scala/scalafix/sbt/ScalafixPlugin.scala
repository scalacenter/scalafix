package scalafix.sbt

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

trait ScalafixKeys {
  val scalafixRewrites: SettingKey[Seq[String]] =
    settingKey[Seq[String]]("Which scalafix rules should run?")
  val scalafixEnabled: SettingKey[Boolean] =
    settingKey[Boolean]("Is scalafix enabled?")
}

object rewrite {
  // TODO(olafur) share these with scalafix-core.
  val ProcedureSyntax = "ProcedureSyntax"
  val ExplicitImplicit = "ExplicitImplicit"
  val VolatileLazyVal = "VolatileLazyVal"
}

object ScalafixPlugin extends AutoPlugin with ScalafixKeys {
  object autoImport extends ScalafixKeys
  private val nightlyVersion = _root_.scalafix.Versions.nightly
  private val disabled = sys.props.contains("scalafix.disable")
  override def requires = JvmPlugin
  override def trigger: PluginTrigger = AllRequirements

  val scalafix: Command = Command.command("scalafix") { state =>
    s"set scalafixEnabled in Global := true" ::
      "clean" ::
        "test:compile" ::
          s"set scalafixEnabled in Global := false" ::
            state
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      addCompilerPlugin("ch.epfl.scala" %% "scalafix-nsc" % nightlyVersion),
      commands += scalafix,
      scalafixRewrites := Seq(
        rewrite.ExplicitImplicit,
        rewrite.ProcedureSyntax
      ),
      scalafixEnabled in Global := false,
      scalacOptions ++= {
        val rewrites = scalafixRewrites.value
        if (!scalafixEnabled.value && rewrites.isEmpty) Nil
        else {
          val prefixed = rewrites.map(x => s"scalafix:$x")
          Seq(s"-P:${prefixed.mkString(",")}")
        }
      }
    )
}
