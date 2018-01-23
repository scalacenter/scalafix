package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta.Symbol
import metaconfig.generic

case class NoInferConfig(symbols: List[Symbol.Global] = Nil) {}

object NoInferConfig {
  implicit val surface = generic.deriveSurface[NoInferConfig]
  val default: NoInferConfig = NoInferConfig()
  implicit val decoder: ConfDecoder[NoInferConfig] =
    generic.deriveDecoder[NoInferConfig](default)
}
