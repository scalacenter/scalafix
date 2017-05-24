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
    cliWrapperClasspath := managedClasspath.in(scalafixStub, Compile).value,
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
        config ++
          rewriteArgs ++
          Seq(
            "--no-sys-exit",
            "-i",
            "--sourceroot",
            baseDirectory.in(ThisBuild).value.getAbsolutePath,
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
  lazy private val scalahostSourcepath: Def.Initialize[Seq[Seq[File]]] =
    Def.settingDyn(
      unmanagedSourceDirectories.all(scalahostAggregateFilter.value))
  lazy private val scalahostClasspath: Def.Initialize[Seq[File]] =
    Def.settingDyn(classDirectory.all(scalahostAggregateFilter.value))
  lazy private val scalahostCompile: Def.Initialize[Task[Seq[Analysis]]] =
    Def.taskDyn(compile.all(scalahostAggregateFilter.value))
  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
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

//object ScalahostPlugin extends AutoPlugin {
//  object autoImport {
//    // use dependsOn(foo % Scalahost) to automatically include the semantic database
//    // of foo in the scala.meta.Mirror() constructor.
//    val Scalameta: Configuration = config("scalameta")
//    val scalametaDependencies: SettingKey[Seq[ProjectRef]] =
//      settingKey[Seq[ProjectRef]](
//        "Projects to analyze with scala.meta, automatically set by dependsOn(foo % Scalahost).")
//  }
//  import autoImport._
//  override def requires = JvmPlugin
//  override def trigger: PluginTrigger = AllRequirements
//  override def projectSettings: Seq[Def.Setting[_]] = scalahostAllSettings
//
//  lazy val scalahostAllSettings =
//    scalahostBaseSettings ++
//      scalahostInjectCompilerPluginSettings ++
//      scalahostHasMirrorSettings
//
//  lazy val scalahostBaseSettings: Seq[Def.Setting[_]] = Def.settings(
//    ivyConfigurations += Scalameta,
//    resolvers += Resolver.bintrayRepo("scalameta", "maven")
//  )
//
//  lazy val scalahostInjectCompilerPluginSettings: Seq[Def.Setting[_]] =
//    Def.settings(
//      libraryDependencies ++= {
//        scalaVersion.value match {
//          case SupportedScalaVersion(version) =>
//            List(
//              "org.scalameta" % s"scalahost_$version" % scalahostVersion % Scalameta
//            )
//          case _ => Nil
//        }
//      },
//      // sets -Xplugin:/scalahost.jar and other necessary compiler options.
//      scalacOptions ++= {
//        scalahostJarPath.value.fold(List.empty[String]) { path =>
//          List(
//            s"-Xplugin:$path",
//            "-Yrangepos",
//            "-Xplugin-require:scalahost"
//          )
//        }
//      }
//    )
//
//  lazy val scalahostHasMirrorSettings: Seq[Def.Setting[_]] = Def.settings(
//    // Automatically set scalametaDependencies to projects listed in dependsOn(x % Scalahost)
//    scalametaDependencies := thisProject.value.dependencies.collect {
//      case x if x.configuration.exists(_ == Scalameta.name) => x.project
//    },
//    javaOptions ++= {
//      if (scalametaDependencies.value.isEmpty) Nil
//      else {
//        val sourcepath =
//          scalahostSourcepath.value.flatten.asPath
//        val classpath =
//          scalahostClasspath.value.asPath
//        val projectName = name.value
//        scalahostJarPath.value.map(path => s"-Dscalahost.jar=$path").toList ++
//          List(
//            s"-D$projectName.scalameta.sourcepath=$sourcepath",
//            s"-D$projectName.scalameta.classpath=$classpath",
//            s"-Dscalameta.sourcepath=$sourcepath",
//            s"-Dscalameta.classpath=$classpath"
//          )
//      }
//    },
//    // fork := true is required to pass along -Dscalameta.mirror.{classpath,sourcepath}
//    fork in run := {
//      if (scalametaDependencies.value.isEmpty) fork.in(run).value
//      else true
//    },
//    // automatically depend on scalahost if this project dependsOn(otherProject % Scalahost)
//    libraryDependencies ++= {
//      if (scalametaDependencies.value.isEmpty) Nil
//      else
//        List(
//          ("org.scalameta" % "scalahost" % scalahostVersion)
//            .cross(CrossVersion.full)
//        )
//    }
//  )
//
//  // Scalahost only supports the latest patch version of scala minor version, for example
//  // 2.11.8 and not 2.11.7. If a build is using 2.11.7, we upgrade the dependency to
//  // scalahost 2.11.8. See https://github.com/scalameta/scalameta/issues/681
//  private object SupportedScalaVersion {
//    private val MajorMinor = "(\\d+\\.\\d+)..*".r
//    def unapply(arg: String): Option[String] = arg match {
//      case MajorMinor(version) =>
//        Versions.supportedScalaVersions.find(_.startsWith(version))
//      case _ => None
//    }
//  }
//
//  private lazy val scalahostJarPath: Def.Initialize[Task[Option[String]]] =
//    Def.task {
//      scalaVersion.value match {
//        case SupportedScalaVersion(version) =>
//          val jarRegex = s".*scalahost_$version(-$scalahostVersion)?.jar$$"
//          val allJars =
//            update.value.filter(configurationFilter(Scalameta.name)).allFiles
//          val scalahostJar = allJars
//            .collectFirst {
//              case file if file.getAbsolutePath.matches(jarRegex) =>
//                file.getAbsolutePath
//            }
//            .getOrElse {
//              throw new IllegalStateException(
//                s"""Unable to find scalahost compiler plugin jar.
//                   |Please report the output below at https://github.com/scalameta/scalameta/issues
//                   |Scala version: ${scalaVersion.value}
//                   |Cross version: ${crossScalaVersions.value}
//                   |Jar regex: $jarRegex
//                   |All jars: $allJars
//                   |""".stripMargin
//              )
//            }
//          Some(scalahostJar)
//        case _ => None
//      }
//    }
//  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
//    def asPath: String =
//      paths.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
//  }
//  // Defaults to version.value of in scala.meta's build.sbt
//  private val scalahostVersion: String =
//    sys.props.getOrElse("scalahost.version", Versions.scalameta)
//  private def scalahostAggregateFilter: Def.Initialize[ScopeFilter] =
//    Def.setting {
//      ScopeFilter(inProjects(scalametaDependencies.value.map(x =>
//                    LocalProject(x.project)): _*),
//                  inConfigurations(Compile, Test, IntegrationTest))
//    }
//  private val scalahostSourcepath: Def.Initialize[Seq[Seq[File]]] =
//    Def.settingDyn(sourceDirectories.all(scalahostAggregateFilter.value))
//  private val scalahostClasspath: Def.Initialize[Seq[File]] =
//    Def.settingDyn(classDirectory.all(scalahostAggregateFilter.value))
//}
