package scalafix.internal.config

import metaconfig.ConfDecoder
import MetaconfigPendingUpstream.XtensionConfScalafix
import org.langmeta.Symbol
import scalafix.internal.util.SymbolOps

case class DisableConfig(symbols: List[CustomMessage[Symbol.Global]] = Nil) {
  def allSymbols = symbols.map(_.value)

  private val messageFromSymbol: Map[String, String] =
    symbols
      .flatMap(custom =>
        custom.message.map(message => (custom.value.syntax, message)))
      .toMap

  def customMessage(symbol: Symbol.Global): Option[String] = {
    messageFromSymbol.get(SymbolOps.normalize(symbol).syntax)
  }

  implicit val customMessageReader: ConfDecoder[CustomMessage[Symbol.Global]] =
    CustomMessage.decoder(field = "symbol")

  implicit val reader: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig](
      _.getField(symbols).map(DisableConfig(_))
    )
}

object DisableConfig {
  val default = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = default.reader
}
