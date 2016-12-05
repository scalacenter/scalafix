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
  private val ScalafixPluginConfig = config("scalafix").hide

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
      ivyConfigurations += ScalafixPluginConfig,
      libraryDependencies ++= {
        if (!scalafixEnabled.value) Nil
        else {
          Seq(
            "ch.epfl.scala" %% "scalafix-nsc" % nightlyVersion % ScalafixPluginConfig
          )
        }
      },
      commands += scalafix,
      scalafixRewrites := Seq(
        rewrite.ExplicitImplicit,
        rewrite.ProcedureSyntax
      ),
      scalafixEnabled in Global := false,
      scalacOptions ++= {
        // scalafix should not affect compilations outside of the scalafix task.
        // The approach taken here is the same as scoverage uses, see:
        // https://github.com/scoverage/sbt-scoverage/blob/45ac49583f5a32dfebdce23b94c5336da4906e59/src/main/scala/scoverage/ScoverageSbtPlugin.scala#L70-L83
        if (!scalafixEnabled.value) Nil
        else {
          val scalafixNscPluginJar = update.value
            .matching(configurationFilter(ScalafixPluginConfig.name))
            .find(_.getAbsolutePath.matches(".*scalafix-nsc[^-]*.jar$"))
            .getOrElse {
              throw new IllegalStateException(
                "Unable to find scalafix-nsc in library dependencies!")
            }
          val rewrites = scalafixRewrites.value
          val config: Option[String] =
            if (rewrites.isEmpty) None
            else {
              val prefixed = rewrites.map(x => s"scalafix:$x")
              Some(s"-P:${prefixed.mkString(",")}")
            }
          Seq(
            Some(s"-Xplugin:${scalafixNscPluginJar.getAbsolutePath}"),
            config
          ).flatten
        }
      }
    )
}
