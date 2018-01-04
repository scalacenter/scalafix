package scalafix.internal.config

import metaconfig.{Conf, ConfDecoder, ConfError, Configured}
import org.langmeta.Symbol

import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix
import scalafix.internal.util._

case class UnlessConfig(
    block: Symbol.Global,
    symbol: Symbol.Global,
    message: Option[String])

object UnlessConfig {
  implicit val decoder: ConfDecoder[UnlessConfig] =
    ConfDecoder.instanceF[UnlessConfig] {
      case c: Conf.Obj =>
        (c.get[Symbol.Global]("block") |@|
          c.get[Symbol.Global]("symbol") |@|
          c.getOption[String]("message")).map {
          case ((a, b), c) => UnlessConfig(a, b, c)
        }
      case _ => Configured.NotOk(ConfError.msg("Wrong config format"))
    }
}

case class DisableUnlessConfig(symbols: List[UnlessConfig] = Nil) {
  import UnlessConfig._

  def allSymbols: List[Symbol.Global] = symbols.map(_.symbol)
  def allBlocks: List[Symbol.Global] = symbols.map(_.block)

  private val messageBySymbol: Map[String, String] =
    (for {
      u <- symbols
      message <- u.message
    } yield {
      SymbolOps.normalize(u.symbol).syntax -> message
    }).toMap

  private val symbolsInBlock_ : Map[String, List[Symbol.Global]] =
    symbols
      .map(u => SymbolOps.normalize(u.block).syntax -> u.symbol)
      .groupBy(_._1)
      .mapValues(_.map(_._2))

  def symbolsInBlock(block: Symbol.Global): List[Symbol.Global] =
    symbolsInBlock_.getOrElse(SymbolOps.normalize(block).syntax, List.empty)

  def customMessage(symbol: Symbol.Global): Option[String] =
    messageBySymbol.get(SymbolOps.normalize(symbol).syntax)

  implicit val reader: ConfDecoder[DisableUnlessConfig] =
    ConfDecoder.instanceF[DisableUnlessConfig](
      _.getField(symbols).map(DisableUnlessConfig(_))
    )
}

object DisableUnlessConfig {
  lazy val default = DisableUnlessConfig()
  implicit val reader: ConfDecoder[DisableUnlessConfig] = default.reader
}
