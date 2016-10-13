/* Modified version of
https://github.com/sbt/sbt-scalariform/blob/61a0b7b75441b458e4ff3c6c30ed87d087a2e569/src/main/scala/com/typesafe/sbt/Scalariform.scala

Original licence:

Copyright 2011-2012 Typesafe Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package scalafix.sbt

import scala.collection.immutable
import scala.collection.immutable.Seq
import scala.util.Failure
import scala.util.Success

import java.net.URLClassLoader

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbt.{IntegrationTest => It}

object ScalafixPlugin extends AutoPlugin {
  val Versions = _root_.scalafix.Versions

  object autoImport {
    lazy val scalafix: TaskKey[Unit] =
      taskKey[Unit]("Fix Scala sources using scalafix")

    lazy val scalafixConfig: TaskKey[Option[File]] =
      taskKey[Option[File]]("Configuration file for scalafix.")

    lazy val hasScalafix: TaskKey[HasScalafix] = taskKey[HasScalafix](
      "Classloaded Scalafix210 instance to overcome 2.10 incompatibility issues.")

    def scalafixSettings: Seq[Setting[_]] =
      noConfigScalafixSettings ++
        inConfig(Compile)(configScalafixSettings) ++
        inConfig(Test)(configScalafixSettings)

    lazy val scalafixSettingsWithIt: Seq[Setting[_]] =
      scalafixSettings ++
        inConfig(IntegrationTest)(configScalafixSettings)

  }
  import autoImport._

  override val projectSettings = scalafixSettings

  override def trigger = allRequirements

  override def requires = JvmPlugin
  lazy val scalafixEnablePersist: TaskKey[Unit] =
    taskKey[Unit]("adds property persist.enable")

  lazy val scalafixDisablePersist: TaskKey[Unit] =
    taskKey[Unit]("removes property persist.enable")

  def noConfigScalafixSettings: Seq[Setting[_]] =
    List(
      addCompilerPlugin(
        Versions.paradiseOrg % "paradise_2.11.8" % Versions.paradiseVersion),
      scalacOptions += "-Ybackend:GenBCode",
      ivyConfigurations += config("scalafix").hide,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-library"     % Versions.scala   % "scalafix",
        "ch.epfl.scala"  % "scalafix-cli_2.11" % Versions.nightly % "scalafix"
      )
    )

  def configScalafixSettings: Seq[Setting[_]] =
    List(
      (sourceDirectories in hasScalafix) := unmanagedSourceDirectories.value,
      includeFilter in Global in hasScalafix := "*.scala",
      scalafixConfig in Global := None,
      hasScalafix := {
        val outputDir = for {
          compilation <- (compile in Compile).value.compilations.allCompilations
          output  <- compilation.outputs().toSeq
        } yield new File(output.outputDirectory())
        val report = update.value
        val jars = report.select(configurationFilter("scalafix"))
        HasScalafix(
          getScalafixLike(new URLClassLoader(jars.map(_.toURI.toURL).toArray,
                                             null),
                          streams.value),
          scalafixConfig.value,
          streams.value,
          (sourceDirectories in hasScalafix).value.toList,
          immutable.Seq(outputDir:_*),
          (includeFilter in hasScalafix).value,
          (excludeFilter in hasScalafix).value,
          thisProjectRef.value)
      },
      scalafixEnablePersist := {
        val props = System.getProperties
        props.setProperty("persist.enable", "")
      },
      scalafixDisablePersist := {
        scalafixEnablePersist.map(_ => compile in Compile).value
      },
      scalafix := {
        scalafixEnablePersist.value
        (clean in Compile).value
        (compile in Compile).value
        hasScalafix.value.writeFormattedContentsToFiles()
        System.getProperties.remove("persist.enable")
      }
    )
  private def getScalafixLike(classLoader: URLClassLoader,
                              streams: TaskStreams): ScalafixLike = {
    val loadedClass =
      new ReflectiveDynamicAccess(classLoader)
        .createInstanceFor[ScalafixLike]("scalafix.cli.Cli210$", Seq.empty)

    loadedClass match {
      case Success(x) => x
      case Failure(e) =>
        streams.log.error(
          s"""Unable to classload Scalafix, please file an issue:
             |https://github.com/scalacenter/scalafix/issues
             |
             |URLs: ${classLoader.getURLs.mkString("\n")}
             |Version: ${_root_.scalafix.Versions.nightly}
             |Error: ${e.getClass}
             |Message: ${e.getMessage}
             |${e.getStackTrace.mkString("\n")}""".stripMargin)
        throw e
    }
  }
}
