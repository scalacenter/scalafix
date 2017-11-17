package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta.Symbol

import MetaconfigPendingUpstream.XtensionConfScalafix

case class DisableConfig(symbols: List[Symbol.Global] = Nil) {
  implicit val reader: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig](
      _.getField(symbols).map(DisableConfig(_))
    )
}

object DisableConfig {
  val default = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = default.reader
}