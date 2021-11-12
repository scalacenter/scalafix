package scalafix.internal.rule

import metaconfig._
import metaconfig.annotation._
import metaconfig.generic.Surface

final case class RedundantTokensConfig(
  @Description("Remove final modifier from objects")
  finalObject: Boolean = true
)

object RedundantTokensConfig {
  val default: RedundantTokensConfig = RedundantTokensConfig()
  implicit val reader: ConfDecoder[RedundantTokensConfig] =
    generic.deriveDecoder[RedundantTokensConfig](default)
  implicit val surface: Surface[RedundantTokensConfig] =
    generic.deriveSurface[RedundantTokensConfig]
}
