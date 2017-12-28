package scalafix.internal.config

import metaconfig.ConfDecoder
import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix

case class FixSyntaxConfig(
    removeFinalVal: Boolean = false,
    addFinalCaseClass: Boolean = false
) {
  implicit val reader: ConfDecoder[FixSyntaxConfig] =
    ConfDecoder.instanceF[FixSyntaxConfig](c =>
      (c.getField(removeFinalVal) |@| c.getField(addFinalCaseClass)).map {
        case (a, b) =>
          FixSyntaxConfig(a, b)
    })
}

object FixSyntaxConfig {
  val default = FixSyntaxConfig()
  implicit val reader: ConfDecoder[FixSyntaxConfig] = default.reader
}
