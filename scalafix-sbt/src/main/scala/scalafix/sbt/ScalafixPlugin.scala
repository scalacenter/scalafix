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

object ScalafixPlugin extends AutoPlugin with ScalafixKeys {
  object autoImport extends ScalafixKeys
  private val Version = "2\\.(\\d\\d)\\..*".r
  private val scalafixVersion = _root_.scalafix.Versions.version
  private val disabled = sys.props.contains("scalafix.disable")
  private def jar(report: UpdateReport): File =
    report.allFiles
      .find { x =>
        x.getAbsolutePath.matches(
          // publishLocal produces jars with name `VERSION/scalafix-nsc_2.11.jar`
          // while the jars published with publishSigned to Maven are named
          // `scalafix-nsc_2.11-VERSION.jar`
          s".*scalafix-nsc_2.1[12](-$scalafixVersion)?.jar$$")
      }
      .getOrElse {
        throw new IllegalStateException(
          s"Unable to resolve scalafix-nsc compiler plugin. Report: $report"
        )
      }

  private def stub(version: String) = {
    val Version(id) = version
    Project(id = s"scalafix-$id", base = file(s"project/scalafix/$id"))
      .settings(
        description :=
          """Serves as a caching layer for extracting the jar location of the
            |scalafix-nsc compiler plugin. If the dependency was added to all
            |projects, the (slow) update task will be re-run for every project.""".stripMargin,
        resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
        publishLocal := {},
        publish := {},
        publishArtifact := false,
        scalaVersion := version,
        libraryDependencies ++= Seq(
          "ch.epfl.scala" %% "scalafix-nsc" % scalafixVersion
        )
      )
  }
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

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile)
  )
  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      commands += scalafix,
      scalafixInternalJar :=
        Def
          .taskDyn[Option[File]] {
            scalaVersion.value match {
              case Version("11") =>
                Def.task(Option(jar((update in scalafix211).value)))
              case Version("12") =>
                Def.task(Option(jar((update in scalafix212).value)))
              case _ =>
                Def.task(None)
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
          val config =
            scalafixConfig.value.map { x =>
              if (!x.isFile)
                streams.value.log.warn(s"File does not exist: $x")
              s"-P:scalafix:${x.getAbsolutePath}"
            }
          scalafixInternalJar.value
            .map { jar =>
              Seq(
                Some(s"-Xplugin:${jar.getAbsolutePath}"),
                Some("-Yrangepos"),
                config
              ).flatten
            }
            .getOrElse(Nil)
        }
      }
    )
}
