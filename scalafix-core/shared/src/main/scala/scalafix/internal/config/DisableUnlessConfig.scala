package scalafix.internal.config

import scalafix.CustomMessage
import scalafix.internal.util._
import metaconfig.annotation.Description
import metaconfig.annotation.ExampleValue
import metaconfig.generic
import metaconfig.generic.Surface
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import org.langmeta.Symbol

case class UnlessConfig(
    @Description("The symbol that indicates a 'safe' block")
    unless: Symbol.Global,
    @Description("The unsafe that are banned unless inside a 'safe' block")
    symbols: List[CustomMessage[Symbol.Global]])

object UnlessConfig {
  implicit val surface: Surface[UnlessConfig] =
    generic.deriveSurface[UnlessConfig]
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

case class DisableUnlessConfig(
    @Description("The list of disable unless configurations")
    @ExampleValue("""[
                    |  {
                    |      unless = "com.IO"
                    |      symbols = [
                    |        {
                    |          symbol = "scala.Predef.println"
                    |          message = "println has side-effects"
                    |        }
                    |      ]
                    |  }
                    |  {
                    |    unless = "scala.util.Try"
                    |    {
                    |      symbol = "scala.Option.get"
                    |      message = "the function may throw an exception"
                    |    }
                    |  }
                    |]""".stripMargin)
    symbols: List[UnlessConfig] = Nil
) {
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

}

object DisableUnlessConfig {
  lazy val default = DisableUnlessConfig()
  implicit val surface: Surface[DisableUnlessConfig] =
    metaconfig.generic.deriveSurface[DisableUnlessConfig]
  implicit val decoder: ConfDecoder[DisableUnlessConfig] =
    generic.deriveDecoder[DisableUnlessConfig](default)
}
