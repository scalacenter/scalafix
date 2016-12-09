package scalafix.sbt

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

trait ScalafixKeys {
  val scalafixConfig: SettingKey[Option[File]] =
    settingKey[Option[File]](
      ".scalafix.conf file to specify which scalafix rules should run.")
  val scalafixEnabled: SettingKey[Boolean] =
    settingKey[Boolean]("Is scalafix enabled?")
  val scalafixInternalJar: TaskKey[Option[File]] =
    taskKey[Option[File]]("Path to scalafix-nsc compiler plugin jar.")
}

object rewrite {
  // TODO(olafur) share these with scalafix-core.
  val ProcedureSyntax = "ProcedureSyntax"
  val ExplicitImplicit = "ExplicitImplicit"
  val VolatileLazyVal = "VolatileLazyVal"
}

object ScalafixPlugin extends AutoPlugin with ScalafixKeys {
  object autoImport extends ScalafixKeys
  private val Version = "2\\.(\\d\\d)\\..*".r
  private val nightlyVersion = _root_.scalafix.Versions.nightly
  private val disabled = sys.props.contains("scalafix.disable")
  private def jar(report: UpdateReport): Option[File] =
    report.allFiles.find(
      _.getAbsolutePath.matches(s".*scalafix-nsc_2.[12].jar$$"))
  private def stub(version: String) =
    Project(id = "scalafix-stub", base = file("project/scalafix")).settings(
      description :=
        """Serves as a caching layer for extracting the jar location of the
          |scalafix-nsc compiler plugin. If the dependecy was added to all
          |projects, the (slow) update task will be re-run for every project.""".stripMargin,
      scalaVersion := version,
      libraryDependencies ++= Seq(
        "ch.epfl.scala" %% "scalafix-nsc" % nightlyVersion
      )
    )
  private val scalafix211 = stub("2.11.8")
  private val scalafix212 = stub("2.12.1")

  override def extraProjects: Seq[Project] = Seq(scalafix211, scalafix212)

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
      scalafixConfig := None,
      scalafixInternalJar :=
        Def
          .taskDyn[Option[File]] {
            scalaVersion.value match {
              case Version("11") =>
                Def.task(jar((update in scalafix211).value))
              case Version("12") =>
                Def.task(jar((update in scalafix212).value))
              case els =>
                Def.task {
                  println("ELS: " + els)
                  None
                }
            }
          }
          .value,
      scalafixEnabled in Global := false,
      scalacOptions ++= {
        // scalafix should not affect compilations outside of the scalafix task.
        // The approach taken here is the same as scoverage uses, see:
        // https://github.com/scoverage/sbt-scoverage/blob/45ac49583f5a32dfebdce23b94c5336da4906e59/src/main/scala/scoverage/ScoverageSbtPlugin.scala#L70-L83
        if (!(scalafixEnabled in Global).value) {
          Nil
        } else {
          scalafixInternalJar.value.map { jar =>
            Seq(
              Some(s"-Xplugin:${jar.getAbsolutePath}"),
              scalafixConfig.value.map(x =>
                s"-P:scalafix:${x.getAbsolutePath}")
            ).flatten
          }.getOrElse(Nil)
        }
      }
    )
}
