package scalafix
package internal.config

import metaconfig._
import metaconfig.annotation.{Description, ExampleValue}
import metaconfig.generic.Surface
import org.langmeta.Symbol

import scalafix.internal.util.SymbolOps

case class DisabledSymbol(
    @Description("Symbol to ban.")
    symbol: Option[Symbol.Global],
    @Description("Custom message.")
    message: Option[String],
    @Description("Custom id for error messages.")
    id: Option[String],
    @Description(
      "If true, additionally ban references " +
        "to methods that override of this symbol. " +
        "Works only with the symbol option, not regex.")
    banOverrides: Boolean,
    @Description(
      "Regex to ban instead of one symbol. " +
        "Supports exclude and include regexes." +
        "Symbol option is forbidden when regex is specified.")
    @ExampleValue("""
                    |{
                    |  include = "java.io.*"
                    |  exclude = "java.io.InputStream"
                    |}""".stripMargin)
    regex: Option[FilterMatcher]) {

  def matches(symbol: Symbol)(implicit index: SemanticdbIndex): Boolean = {
    this.symbol match {
      case Some(s) => SymbolOps.isSameNormalized(symbol, s) || (
        banOverrides &&
          index
            .denotation(symbol)
            .exists(_.overrides.exists(SymbolOps.isSameNormalized(symbol, _)))
        )
      case None =>
        regex match {
          case Some(r) => r.matches(symbol.toString)
          case None => sys.error("impossible")
        }
    }
  }
}

object DisabledSymbol {
  implicit val surface: Surface[DisabledSymbol] =
    generic.deriveSurface[DisabledSymbol]

  private def normalizeMessage(msg: String): String =
    if (msg.isMultiline) {
      "\n" + msg.stripMargin
    } else {
      msg
    }
  implicit val reader: ConfDecoder[DisabledSymbol] =
    ConfDecoder.instanceF[DisabledSymbol] {
      case c: Conf.Obj =>
        (c.getOption[Symbol.Global]("symbol") |@|
          c.getOption[String]("message") |@|
          c.getOption[String]("id") |@|
          c.getOrElse[Boolean]("banOverrides")(false) |@|
          c.getOption[FilterMatcher]("regex"))
          .andThen {
            case ((((Some(_), b), c), d), Some(_)) =>
              Configured.notOk(
                ConfError.message("Symbol and regex are both specified."))
            case ((((a @ Some(_), b), c), d), None) =>
              Configured.ok(
                DisabledSymbol(a, b.map(normalizeMessage), c, d, None))
            case ((((None, b), c), d), e @ Some(_)) =>
              Configured.ok(
                DisabledSymbol(None, b.map(normalizeMessage), c, d, e))
            case ((((None, b), c), d), None) =>
              Configured.notOk(
                ConfError.message("Symbol and regex are both not specified."))
          }
      case s: Conf.Str =>
        symbolGlobalReader
          .read(s)
          .map(sym => DisabledSymbol(Some(sym), None, None, false, None))
      case _ => Configured.NotOk(ConfError.message("Wrong config format"))
    }
}

case class UnlessInsideBlock(
    @Description("The symbol that indicates a 'safe' block.")
    safeBlock: DisabledSymbol,
    @Description(
      "The unsafe symbols that are banned unless inside a 'safe' block")
    symbols: List[DisabledSymbol])

object UnlessInsideBlock {
  implicit val surface: Surface[UnlessInsideBlock] =
    generic.deriveSurface[UnlessInsideBlock]
  // NOTE(olafur): metaconfig.generic.deriveDecoder requires a default base values.
  // Here we require all fields to be provided by the user so we write the decoder manually.
  implicit val reader: ConfDecoder[UnlessInsideBlock] =
    ConfDecoder.instanceF[UnlessInsideBlock] {
      case c: Conf.Obj =>
        (c.get[DisabledSymbol]("safeBlock") |@|
          c.get[List[DisabledSymbol]]("symbols")).map {
          case (a, b) => UnlessInsideBlock(a, b)
        }
      case _ => Configured.NotOk(ConfError.message("Wrong config format"))
    }
}

case class DisableConfig(
    @Description("List of symbols to disable if written explicitly in source." +
      " Does not report an error for inferred symbols in macro expanded code " +
      "or implicits.")
    @ExampleValue("""
                    |[
                    |  {
                    |    symbol = "scala.Any.asInstanceOf"
                    |    message = "use pattern-matching instead"
                    |  }
                    |]""".stripMargin)
    symbols: List[DisabledSymbol] = Nil,
    @Description(
      "List of symbols to disable if inferred. Does not report an error for symbols written explicitly in source code.")
    @ExampleValue("""
                    |[
                    |  {
                    |    symbol = "scala.Predef.any2stringadd"
                    |    message = "use explicit toString be calling +"
                    |  }
                    |]""".stripMargin)
    ifSynthetic: List[DisabledSymbol] = Nil,
    @Description(
      "List of symbols to disable unless they are in the given block.")
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
  lazy val allSafeBlocks: List[DisabledSymbol] =
    unlessInside.map(_.safeBlock)
  lazy val allDisabledSymbols: List[DisabledSymbol] =
    symbols ++ ifSynthetic ++ unlessInside.flatMap(_.symbols)
}

object DisableConfig {
  lazy val default = DisableConfig()
  implicit val surface: Surface[DisableConfig] =
    metaconfig.generic.deriveSurface[DisableConfig]
  implicit val decoder: ConfDecoder[DisableConfig] =
    generic.deriveDecoder[DisableConfig](default)
}
