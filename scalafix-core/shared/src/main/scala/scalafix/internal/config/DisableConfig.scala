package scalafix
package internal.config

import metaconfig.ConfDecoder
import MetaconfigPendingUpstream.XtensionConfScalafix
import org.langmeta.Symbol
import scalafix.internal.util.SymbolOps

case class DisableConfig(symbols: List[CustomMessage[Symbol.Global]] = Nil) {
  def allSymbols: List[Symbol.Global] = symbols.map(_.value)

  private val messageBySymbol: Map[String, CustomMessage[Symbol.Global]] =
    symbols
      .map(custom => (SymbolOps.normalize(custom.value).syntax, custom))
      .toMap

  def customMessage(
      symbol: Symbol.Global): Option[CustomMessage[Symbol.Global]] =
    messageBySymbol.get(SymbolOps.normalize(symbol).syntax)

  implicit val customMessageReader: ConfDecoder[CustomMessage[Symbol.Global]] =
    CustomMessage.decoder(field = "symbol")

  implicit val reader: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig](
      _.getField(symbols).map(DisableConfig(_))
    )
}

object DisableConfig {
  val default: DisableConfig = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = default.reader
}
