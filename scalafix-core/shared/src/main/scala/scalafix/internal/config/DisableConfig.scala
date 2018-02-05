package scalafix
package internal.config

import metaconfig._
import org.langmeta.Symbol

import scalafix.internal.util.SymbolOps
import metaconfig.annotation.{DeprecatedName, Description, ExampleValue}
import metaconfig.generic.Surface

case class DisablePart(
    @Description("The symbol that indicates a 'safe' block. Optional value.")
    @DeprecatedName(
      "unless",
      "Renamed to unlessInsideBlock",
      "0.6.0"
    )
    unlessInsideBlock: Option[Symbol.Global],
    @Description(
      "When true also blocks symbols in the generated code. Default to false. " +
        "Ignored if unlessInsideBlock is specified.")
    unlessSynthetic: Boolean,
    @Description("The unsafe that are banned unless inside a 'safe' block")
    symbols: List[CustomMessage[Symbol.Global]])

object DisablePart {
  implicit val surface: Surface[DisablePart] =
    generic.deriveSurface[DisablePart]
  // NOTE(olafur): metaconfig.generic.deriveDecoder requires a default base values.
  // Here we require all fields to be provided by the user so we write the decoder manually.
  implicit val reader: ConfDecoder[DisablePart] =
    ConfDecoder.instanceF[DisablePart] {
      case c: Conf.Obj =>
        (c.getOption[Symbol.Global]("unlessInsideBlock", "unless") |@|
          c.getOrElse[Boolean]("unlessSynthetic")(false) |@|
          c.get[List[CustomMessage[Symbol.Global]]]("symbols")).map {
          case ((a @ Some(_), _), c) => DisablePart(a, false, c)
          // when unlessInsideBlock is specified we ignore unlessSynthetic
          case ((None, b), c) => DisablePart(None, b, c)
        }
      case _ => Configured.NotOk(ConfError.message("Wrong config format"))
    }
}

case class DisableConfig(
    @Description("The list of disable configurations")
    @ExampleValue("""[
                    |  { // default mode
                    |    symbols = [
                    |      "scala.Any.asInstanceOf"
                    |    ]
                    |  }
                    |  { // unlessSynthetic mode
                    |    unlessSynthetic = true
                    |    symbols = [
                    |      {
                    |        symbol = "scala.Predef.any2stringadd"
                    |        message = "Use explicit toString be calling +"
                    |      }
                    |    ]
                    |  }
                    |  { // unlessInsideBlock mode
                    |    unlessInsideBlock = "com.IO"
                    |    symbols = [
                    |      {
                    |        symbol = "scala.Predef.println"
                    |        message = "println has side-effects"
                    |      }
                    |    ]
                    |  }
                    |  { // unlessInsideBlock mode
                    |     unlessInsideBlock = "scala.util.Try"
                    |     symbols = [
                    |       {
                    |          symbol = "scala.Option.get"
                    |          message = "the function may throw an exception"
                    |       }
                    |     ]
                    |  }
                    |]""".stripMargin)
    parts: List[DisablePart] = Nil
) {
  private def normalizeSymbol(symbol: Symbol.Global): String =
    SymbolOps.normalize(symbol).syntax

  def allUnlessBlocks: List[Symbol.Global] =
    parts.flatMap(_.unlessInsideBlock)
  def allSymbols: List[Symbol.Global] = parts.flatMap(_.symbols.map(_.value))
  def allSymbolsInSynthetics: List[Symbol.Global] =
    for {
      part <- parts
      if part.unlessSynthetic
      s <- part.symbols
    } yield s.value

  private val syntheticFlagBySymbol: Map[String, Boolean] =
    (for {
      p <- parts
      s <- p.symbols
    } yield {
      normalizeSymbol(s.value) -> p.unlessSynthetic
    }).toMap

  private val messageBySymbol: Map[String, CustomMessage[Symbol.Global]] =
    (for {
      p <- parts
      s <- p.symbols
    } yield {
      normalizeSymbol(s.value) -> s
    }).toMap

  private val symbolsByUnless: Map[String, List[Symbol.Global]] = {
    val block2Symbols = for {
      part <- parts
      u <- part.unlessInsideBlock
    } yield normalizeSymbol(u) -> part.symbols.map(_.value)

    block2Symbols.groupBy(_._1).mapValues(_.flatMap(_._2))
  }

  def symbolsInUnlessBlock(unless: Symbol.Global): List[Symbol.Global] =
    symbolsByUnless.getOrElse(normalizeSymbol(unless), List.empty)

  def customMessage(
      symbol: Symbol.Global): Option[CustomMessage[Symbol.Global]] =
    messageBySymbol.get(normalizeSymbol(symbol))

  def syntheticFlag(symbol: Symbol.Global): Boolean = {
    syntheticFlagBySymbol.getOrElse(normalizeSymbol(symbol), false)
  }
}

object DisableConfig {
  lazy val default = DisableConfig()
  implicit val surface: Surface[DisableConfig] =
    metaconfig.generic.deriveSurface[DisableConfig]
  implicit val decoder: ConfDecoder[DisableConfig] =
    generic.deriveDecoder[DisableConfig](default)
}
