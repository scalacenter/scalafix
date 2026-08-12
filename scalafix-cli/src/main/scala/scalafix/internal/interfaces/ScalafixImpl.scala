package scalafix.internal.interfaces

import buildinfo.RulesBuildInfo
import scalafix.Versions
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixArguments
import scalafix.internal.reflect.ScalafixToolbox
import scalafix.internal.v1.Args
import scalafix.internal.v1.MainOps

final class ScalafixImpl extends Scalafix {

  // One toolbox per Scalafix instance: dropping the instance drops its
  // compiled-rule caches (https://github.com/scalacenter/scalafix/issues/782).
  private val toolbox = new ScalafixToolbox

  override def toString: String =
    s"""Scalafix v${scalafixVersion()}"""

  override def newArguments(): ScalafixArguments =
    ScalafixArgumentsImpl(Args.default.copy(toolbox = toolbox))

  override def mainHelp(screenWidth: Int): String = {
    MainOps.helpMessage(screenWidth)
  }

  override def scalaVersion(): String =
    RulesBuildInfo.scalaVersion
  override def scalafixVersion(): String =
    Versions.version
  override def scalametaVersion(): String =
    Versions.scalameta
  override def supportedScalaVersions(): Array[String] =
    Versions.supportedScalaVersions.toArray
  override def scala211(): String =
    throw new java.lang.UnsupportedOperationException(
      "Scala 2.11 is no longer supported; the final version supporting it is Scalafix 0.10.4"
    )
  override def scala212(): String =
    Versions.scala212
  override def scala213(): String =
    Versions.scala213
  override def scala33(): String =
    Versions.scala33
  override def scala35(): String =
    Versions.scala35
  override def scala36(): String =
    Versions.scala36
  override def scala37(): String =
    Versions.scala37
  override def scala38(): String =
    Versions.scala38
  override def scala3LTS(): String =
    Versions.scala3LTS
  override def scala3Next(): String =
    Versions.scala3Next

}
