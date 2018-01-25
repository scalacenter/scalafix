package scalafix
package internal.config

import metaconfig.ConfDecoder
import org.langmeta.Symbol
import scalafix.internal.util.SymbolOps
import metaconfig.generic
import metaconfig.generic.Surface

case class DisableConfig(symbols: List[CustomMessage[Symbol.Global]] = Nil) {
  def allSymbols: List[Symbol.Global] = symbols.map(_.value)

  private val messageBySymbol: Map[String, CustomMessage[Symbol.Global]] =
    symbols
      .map(custom => (SymbolOps.normalize(custom.value).syntax, custom))
      .toMap

  def customMessage(
      symbol: Symbol.Global): Option[CustomMessage[Symbol.Global]] =
    messageBySymbol.get(SymbolOps.normalize(symbol).syntax)
}

object DisableConfig {
  val default: DisableConfig = DisableConfig()
  implicit val surface: Surface[DisableConfig] =
    generic.deriveSurface[DisableConfig]
  implicit val decoder: ConfDecoder[DisableConfig] =
    generic.deriveDecoder[DisableConfig](default)
}
