package scalafix.internal.config

import metaconfig.generic

case class DebugConfig(
    printSymbols: Boolean = false
)
object DebugConfig {
  implicit val surface = generic.deriveSurface[DebugConfig]
  val default = DebugConfig()
  implicit val decoder = generic.deriveDecoder[DebugConfig](default)
}
