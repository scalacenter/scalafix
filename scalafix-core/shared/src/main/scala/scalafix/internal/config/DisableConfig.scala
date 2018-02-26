package scalafix
package internal.config

import metaconfig._
import org.langmeta.Symbol

import scalafix.internal.util.SymbolOps
import metaconfig.annotation.{Description, ExampleValue}
import metaconfig.generic.Surface

case class UnlessInsideBlock(
    @Description("The symbol that indicates a 'safe' block.")
    safeBlock: Symbol.Global,
    @Description(
      "The unsafe symbols that are banned unless inside a 'safe' block")
    symbols: List[CustomMessage[Symbol.Global]])

object UnlessInsideBlock {
  implicit val surface: Surface[UnlessInsideBlock] =
    generic.deriveSurface[UnlessInsideBlock]
  // NOTE(olafur): metaconfig.generic.deriveDecoder requires a default base values.
  // Here we require all fields to be provided by the user so we write the decoder manually.
  implicit val reader: ConfDecoder[UnlessInsideBlock] =
    ConfDecoder.instanceF[UnlessInsideBlock] {
      case c: Conf.Obj =>
        (c.get[Symbol.Global]("safeBlock") |@|
          c.get[List[CustomMessage[Symbol.Global]]]("symbols")).map {
          case (a, b) => UnlessInsideBlock(a, b)
        }
      case _ => Configured.NotOk(ConfError.message("Wrong config format"))
    }
}

case class DisableConfig(
    @Description("The list of symbols to disable only in the actual sources.")
    @ExampleValue("""
                    |[
                    |  {
                    |    symbol = "scala.Any.asInstanceOf"
                    |    message = "use pattern-matching instead"
                    |  }
                    |]""".stripMargin)
    symbols: List[CustomMessage[Symbol.Global]] = Nil,
    @Description(
      "The list of symbols to disable, also blocks symbols in the generated code")
    @ExampleValue("""
                    |[
                    |  {
                    |    symbol = "scala.Predef.any2stringadd"
                    |    message = "use explicit toString be calling +"
                    |  }
                    |]""".stripMargin)
    ifSynthetic: List[CustomMessage[Symbol.Global]] = Nil,
    @Description(
      "The list of symbols to disable unless they are in the given block.")
    @ExampleValue("""
                    |[
                    |  {
                    |    safeBlock = "scala.util.Try"
                    |    symbols = [
                    |      {
                    |        symbol = "scala.Option.get"
                    |        message = "the function may throw an exception"
                    |      }
                    |    ]
                    |  }
                    |]
      """.stripMargin)
    unlessInside: List[UnlessInsideBlock] = Nil
) {
  private def normalizeSymbol(symbol: Symbol.Global): String =
    SymbolOps.normalize(symbol).syntax

  lazy val allUnlessBlocks: List[Symbol.Global] =
    unlessInside.map(_.safeBlock)
  private lazy val allCustomMessages: List[CustomMessage[Symbol.Global]] =
    symbols ++ ifSynthetic ++ unlessInside.flatMap(_.symbols)
  lazy val allSymbols: List[Symbol.Global] = allCustomMessages.map(_.value)
  lazy val allSymbolsInSynthetics: List[Symbol.Global] =
    ifSynthetic.map(_.value)

  private val syntheticSymbols: Set[String] =
    ifSynthetic.map(u => normalizeSymbol(u.value)).toSet

  private val messageBySymbol: Map[String, CustomMessage[Symbol.Global]] =
    allCustomMessages.map(m => normalizeSymbol(m.value) -> m).toMap

  private val symbolsByUnless: Map[String, List[Symbol.Global]] =
    unlessInside
      .groupBy(u => normalizeSymbol(u.safeBlock))
      .mapValues(_.flatMap(_.symbols.map(_.value)))

  def symbolsInSafeBlock(unless: Symbol.Global): List[Symbol.Global] =
    symbolsByUnless.getOrElse(normalizeSymbol(unless), List.empty)

  def customMessage(
      symbol: Symbol.Global): Option[CustomMessage[Symbol.Global]] =
    messageBySymbol.get(normalizeSymbol(symbol))

  def isSynthetic(symbol: Symbol.Global): Boolean = {
    syntheticSymbols.contains(normalizeSymbol(symbol))
  }
}

object DisableConfig {
  lazy val default = DisableConfig()
  implicit val surface: Surface[DisableConfig] =
    metaconfig.generic.deriveSurface[DisableConfig]
  implicit val decoder: ConfDecoder[DisableConfig] =
    generic.deriveDecoder[DisableConfig](default)
}
