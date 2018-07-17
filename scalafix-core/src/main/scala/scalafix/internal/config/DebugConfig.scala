package scalafix.internal.config

import metaconfig.generic
import metaconfig.ConfDecoder
import metaconfig.generic.Surface

case class DebugConfig(
    printSymbols: Boolean = false
)
object DebugConfig {
  implicit val surface: Surface[DebugConfig] =
    generic.deriveSurface[DebugConfig]
  val default = DebugConfig()
  implicit val decoder: ConfDecoder[DebugConfig] =
    generic.deriveDecoder[DebugConfig](default)
}
