package scalafix
package internal.config

import metaconfig._
import org.langmeta.Symbol

import scalafix.internal.util.SymbolOps
import metaconfig.annotation.{Description, ExampleValue}
import metaconfig.generic.Surface

import scala.util.matching.Regex

case class DisabledSymbol(
    @Description("Symbol to ban.")
    symbol: Option[Symbol.Global],
    @Description("Custom message.")
    message: Option[String],
    @Description("Custom id for error messages.")
    id: Option[String],
    @Description(
      "When true all overrides of the symbol are also banned. Works only with symbol field, not regex. Doesn't work in unlessSynthetic mode.")
    banHierarchy: Boolean,
    @Description(
      "Regex to ban instead of one symbol. When symbol field is specified, this field is ignored.")
    regex: Option[String]) {

  lazy val compiledRegex: Option[Regex] = regex.map(_.r)
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
          c.getOrElse[Boolean]("banHierarchy")(false) |@|
          c.getOption[String]("regex"))
          .map {
            case ((((a @ Some(_), b), c), d), _) =>
              DisabledSymbol(a, b.map(normalizeMessage), c, d, None)
            case ((((None, b), c), d), e) =>
              DisabledSymbol(None, b.map(normalizeMessage), c, d, e)
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
