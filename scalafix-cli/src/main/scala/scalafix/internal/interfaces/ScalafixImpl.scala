package scalafix.internal.interfaces

import scalafix.Versions
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixArguments
import scalafix.internal.v1.MainOps

final class ScalafixImpl extends Scalafix {

  override def toString: String =
    s"""Scalafix v${scalafixVersion()}"""

  override def newArguments(): ScalafixArguments =
    ScalafixArgumentsImpl()

  override def mainHelp(screenWidth: Int): String = {
    MainOps.helpMessage(screenWidth)
  }

  override def scalafixVersion(): String =
    Versions.version
  override def scalametaVersion(): String =
    Versions.scalameta
  override def supportedScalaVersions(): Array[String] =
    Versions.supportedScalaVersions.toArray
  override def scala211(): String =
    Versions.scala211
  override def scala212(): String =
    Versions.scala212
  override def scala213(): String =
    Versions.scala213

}
