package scalafix.internal.sbt

object SBTCompat {
  object CrossVersion {
    def partialVersion(version: String): Option[(Long, Long)] =
      sbt.CrossVersion.partialVersion(version)
  }
}