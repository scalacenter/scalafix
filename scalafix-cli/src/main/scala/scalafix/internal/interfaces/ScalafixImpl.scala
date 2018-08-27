package scalafix.internal.interfaces

import scalafix.Versions
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError
import scalafix.interfaces.ScalafixMainArgs
import scalafix.internal.v1.MainOps
import scalafix.internal.v1.Rules

final class ScalafixImpl extends Scalafix {

  override def toString: String =
    s"""Scalafix v${scalafixVersion()}"""

  override def runMain(args: ScalafixMainArgs): Array[ScalafixError] = {
    val exit =
      MainOps.run(Array(), args.asInstanceOf[ScalafixMainArgsImpl].args)
    ScalafixErrorImpl.fromScala(exit)
  }

  override def newMainArgs(): ScalafixMainArgs =
    ScalafixMainArgsImpl()

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

}
