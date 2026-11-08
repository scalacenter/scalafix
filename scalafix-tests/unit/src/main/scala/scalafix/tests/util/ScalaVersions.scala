package scalafix.tests.util

import scalafix.tests.UnitBuildInfo

object ScalaVersions {
  def isScala212: Boolean =
    UnitBuildInfo.scalaVersion.startsWith("2.12")
  def isScala213: Boolean =
    UnitBuildInfo.scalaVersion.startsWith("2.13")
  def isScala3: Boolean =
    UnitBuildInfo.scalaVersion.startsWith("3")
}
