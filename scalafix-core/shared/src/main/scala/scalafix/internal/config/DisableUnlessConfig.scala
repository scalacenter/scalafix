package scalafix.internal.config

import metaconfig.{Conf, ConfDecoder, ConfError, Configured}
import org.langmeta.Symbol

import scalafix.CustomMessage
import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix
import scalafix.internal.util._

case class UnlessConfig(
    unless: Symbol.Global,
    symbols: List[CustomMessage[Symbol.Global]])

object UnlessConfig {
  // NOTE(olafur): metaconfig.generic.deriveDecoder requires a default base values.
  // Here we require all fields to be provided by the user so we write the decoder manually.
  implicit val reader: ConfDecoder[UnlessConfig] =
    ConfDecoder.instanceF[UnlessConfig] {
      case c: Conf.Obj =>
        (c.get[Symbol.Global]("unless") |@|
          c.get[List[CustomMessage[Symbol.Global]]]("symbols")).map {
          case (a, b) => UnlessConfig(a, b)
        }
      case _ => Configured.NotOk(ConfError.message("Wrong config format"))
    }
}

case class DisableUnlessConfig(symbols: List[UnlessConfig] = Nil) {

  private def normalizeSymbol(symbol: Symbol.Global): String =
    SymbolOps.normalize(symbol).syntax

  def allUnless: List[Symbol.Global] = symbols.map(_.unless)
  def allSymbols: List[Symbol.Global] = symbols.flatMap(_.symbols.map(_.value))

  private val messageBySymbol: Map[String, String] =
    (for {
      u <- symbols
      s <- u.symbols
      message <- s.message
    } yield {
      normalizeSymbol(s.value) -> message
    }).toMap

  private val symbolsByUnless: Map[String, List[Symbol.Global]] =
    symbols
      .map(u => normalizeSymbol(u.unless) -> u.symbols.map(_.value))
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
