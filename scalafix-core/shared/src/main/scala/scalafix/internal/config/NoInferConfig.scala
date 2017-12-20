package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta.Symbol

import MetaconfigPendingUpstream.XtensionConfScalafix

case class NoInferConfig(symbols: List[Symbol.Global] = Nil) {
  implicit val reader: ConfDecoder[NoInferConfig] =
    ConfDecoder.instanceF[NoInferConfig](
      _.getField(symbols).map(NoInferConfig(_))
    )
}

object NoInferConfig {
  val default: NoInferConfig = NoInferConfig()
  implicit val reader: ConfDecoder[NoInferConfig] = default.reader
}