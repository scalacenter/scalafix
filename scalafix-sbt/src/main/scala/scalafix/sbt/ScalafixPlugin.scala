package scalafix.sbt

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

trait ScalafixKeys {
  val scalafixRewrites: SettingKey[Seq[String]] =
    settingKey[Seq[String]]("Which scalafix rules should run?")
  val scalafixEnabled: SettingKey[Boolean] =
    settingKey[Boolean]("Is scalafix enabled?")
  val scalafixInternalJar: TaskKey[File] =
    taskKey[File]("Path to scalafix-nsc compiler plugin jar.")
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
  private val scalafixStub =
    Project(id = "scalafix-stub", base = file("project/scalafix")).settings(
      description :=
        """Serves as a caching layer for extracting the jar location of the
          |scalafix-nsc compiler plugin. If the dependecy was added to all
          |projects, the (slow) update task will be re-run for every project.""".stripMargin,
      scalaVersion := "2.11.8", // TODO(olafur) 2.12 support
      libraryDependencies +=
        "ch.epfl.scala" %% "scalafix-nsc" % nightlyVersion % Compile
    )

  override def extraProjects: Seq[Project] = Seq(scalafixStub)

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
      commands += scalafix,
      scalafixRewrites := Seq(
        rewrite.ExplicitImplicit,
        rewrite.ProcedureSyntax
      ),
      scalafixInternalJar in Global := {
        // TODO(olafur) 2.12 support
        (update in scalafixStub).value.allFiles
          .find(_.getAbsolutePath.matches(".*scalafix-nsc[^-]*.jar$"))
          .getOrElse {
            throw new IllegalStateException(
              "Unable to find scalafix-nsc in library dependencies!")
          }
      },
      scalafixEnabled in Global := false,
      scalacOptions ++= {
        // scalafix should not affect compilations outside of the scalafix task.
        // The approach taken here is the same as scoverage uses, see:
        // https://github.com/scoverage/sbt-scoverage/blob/45ac49583f5a32dfebdce23b94c5336da4906e59/src/main/scala/scoverage/ScoverageSbtPlugin.scala#L70-L83
        if (!scalafixEnabled.value || scalaVersion.value.startsWith("2.10")) {
          Nil
        } else {
          val rewrites = scalafixRewrites.value
          val config: Option[String] =
            if (rewrites.isEmpty) None
            else {
              val prefixed = rewrites.map(x => s"scalafix:$x")
              Some(s"-P:${prefixed.mkString(",")}")
            }
          val jar = (scalafixInternalJar in Global).value.getAbsolutePath
          Seq(
            Some(s"-Xplugin:$jar"),
            config
          ).flatten
        }
      }
    )
}
