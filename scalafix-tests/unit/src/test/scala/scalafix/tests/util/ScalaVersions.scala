package scalafix.tests.util

import scalafix.tests.BuildInfo

object ScalaVersions {
  def isScala212: Boolean =
    BuildInfo.scalaVersion.startsWith("2.12")
  def isScala213: Boolean =
    BuildInfo.scalaVersion.startsWith("2.13")
  def isScala3: Boolean =
    BuildInfo.scalaVersion.startsWith("3")
}
