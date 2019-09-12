package scalafix.tests.util

object ScalaVersions {
  def isScala211: Boolean =
    util.Properties.versionNumberString.startsWith("2.11")
  def isScala212: Boolean =
    util.Properties.versionNumberString.startsWith("2.12")
  def isScala213: Boolean =
    util.Properties.versionNumberString.startsWith("2.13")
}
