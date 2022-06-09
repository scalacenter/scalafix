package scalafix.internal.rule

import metaconfig._
import metaconfig.annotation._
import metaconfig.generic.Surface

final case class RedundantSyntaxConfig(
    @Description("Remove final modifier from objects")
    finalObject: Boolean = true,
    @Description("Remove unnecessary string interpolator")
    stringInterpolator: Boolean = true
)

object RedundantSyntaxConfig {
  val default: RedundantSyntaxConfig = RedundantSyntaxConfig()
  implicit val reader: ConfDecoder[RedundantSyntaxConfig] =
    generic.deriveDecoder[RedundantSyntaxConfig](default)
  implicit val surface: Surface[RedundantSyntaxConfig] =
    generic.deriveSurface[RedundantSyntaxConfig]
}
