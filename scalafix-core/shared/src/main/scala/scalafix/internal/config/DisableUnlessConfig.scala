package scalafix.internal.config

import metaconfig.{Conf, ConfDecoder, ConfError, Configured}
import org.langmeta.Symbol

import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix
import scalafix.internal.util._

case class UnlessConfigSymbol(symbol: Symbol.Global, message: Option[String])

object UnlessConfigSymbol {
  implicit val decoder: ConfDecoder[UnlessConfigSymbol] =
    ConfDecoder.instanceF[UnlessConfigSymbol] {
      case c: Conf.Obj =>
        (c.get[Symbol.Global]("symbol") |@|
          c.getOption[String]("message")).map {
          case (a, b) => UnlessConfigSymbol(a, b)
        }
      case els =>
        implicitly[ConfDecoder[Symbol.Global]]
          .read(els)
          .map(UnlessConfigSymbol(_, None))
    }
}

case class UnlessConfig(
    unless: Symbol.Global,
    symbols: List[UnlessConfigSymbol])

object UnlessConfig {
  implicit val decoder: ConfDecoder[UnlessConfig] =
    ConfDecoder.instanceF[UnlessConfig] {
      case c: Conf.Obj =>
        (c.get[Symbol.Global]("unless") |@|
          c.get[List[UnlessConfigSymbol]]("symbols")).map {
          case (a, b) => UnlessConfig(a, b)
        }
      case _ => Configured.NotOk(ConfError.msg("Wrong config format"))
    }
}

case class DisableUnlessConfig(symbols: List[UnlessConfig] = Nil) {
  import UnlessConfig._

  @inline private def normalizeSymbol(symbol: Symbol.Global): String =
    SymbolOps.normalize(symbol).syntax

  def allUnless: List[Symbol.Global] = symbols.map(_.unless)
  def allSymbols: List[Symbol.Global] = symbols.flatMap(_.symbols.map(_.symbol))

  private val messageBySymbol: Map[String, String] =
    (for {
      u <- symbols
      s <- u.symbols
      message <- s.message
    } yield {
      normalizeSymbol(s.symbol) -> message
    }).toMap

  private val symbolsByUnless: Map[String, List[Symbol.Global]] =
    symbols
      .map(u => normalizeSymbol(u.unless) -> u.symbols.map(_.symbol))
      .groupBy(_._1)
      .mapValues(_.flatMap(_._2))

  def symbolsInUnless(unless: Symbol.Global): List[Symbol.Global] =
    symbolsByUnless.getOrElse(normalizeSymbol(unless), List.empty)

  def customMessage(symbol: Symbol.Global): Option[String] =
    messageBySymbol.get(normalizeSymbol(symbol))

  implicit val reader: ConfDecoder[DisableUnlessConfig] =
    ConfDecoder.instanceF[DisableUnlessConfig](
      _.getField(symbols).map(DisableUnlessConfig(_))
    )
}

object DisableUnlessConfig {
  lazy val default = DisableUnlessConfig()
  implicit val reader: ConfDecoder[DisableUnlessConfig] = default.reader
}
