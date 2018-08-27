package scalafix.internal.rule

import metaconfig.ConfDecoder
import metaconfig.annotation.Description
import metaconfig.generic
import metaconfig.generic.Surface

final case class MissingFinalConfig(
    @Description(
      "Report error on a missing `final` modifier for non-abstract classes " +
        "that extend a sealed class.")
    noLeakingSealed: Boolean = true,
    @Description("Insert the final modifier to case classes.")
    noLeakingCaseClass: Boolean = true
)

object MissingFinalConfig {
  val default = MissingFinalConfig()
  implicit val surface: Surface[MissingFinalConfig] =
    generic.deriveSurface[MissingFinalConfig]
  implicit val decoder: ConfDecoder[MissingFinalConfig] =
    generic.deriveDecoder[MissingFinalConfig](default)
}
