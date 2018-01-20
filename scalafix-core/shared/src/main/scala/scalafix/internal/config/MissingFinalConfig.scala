package scalafix.internal.config

import metaconfig.ConfDecoder

import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix

case class MissingFinalConfig(
    finalCaseClass: Boolean = false,
    finalClass: Boolean = false
) {
  implicit val reader: ConfDecoder[MissingFinalConfig] =
    ConfDecoder.instanceF[MissingFinalConfig](
      c =>
        (
          c.getField(finalCaseClass) |@|
            c.getField(finalClass)
        ).map {
          case (a, b) => MissingFinalConfig(a, b)
      })
}

object MissingFinalConfig {
  val default: MissingFinalConfig = MissingFinalConfig()
  implicit val reader: ConfDecoder[MissingFinalConfig] = default.reader
}
