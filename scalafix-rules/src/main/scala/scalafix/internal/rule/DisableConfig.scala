package scalafix.internal.rule

import metaconfig._
import metaconfig.annotation.{Description, ExampleValue}
import metaconfig.generic.Surface
import scalafix.v0.Symbol
import scalafix.internal.util.SymbolOps
import scalafix.internal.config._

case class DisabledSymbol(
    @ExampleValue("scala.sys.process.Process")
    @Description(
      "A single symbol to ban. Cannot be used together with the regex option.")
    symbol: Option[Symbol.Global],
    @Description("Custom message.")
    message: Option[String],
    @Description("Custom id for error messages.")
    id: Option[String],
    @Description(
      "Regex to ban symbols matching a given include/exclude patterns." +
        "Cannot be used together with the symbol option." +
        "Include and exclude filters can be either a list of regex strings or a single regex string.")
    @ExampleValue("""
                    |{
                    |  includes = [
                    |    "java.io.*"
                    |    "scala.io.*"
                    |  ]
                    |  excludes = "java.io.InputStream"
                    |}""".stripMargin)
    regex: Option[FilterMatcher]) {

  def matches(symbol: Symbol): Boolean = {
    this.symbol match {
      case Some(s) =>
        SymbolOps.isSameNormalized(symbol, s)
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
    if (msg.contains("\n")) {
      "\n" + msg.stripMargin
    } else {
      msg
    }
  implicit val reader: ConfDecoder[DisabledSymbol] =
    ConfDecoder.instance[DisabledSymbol] {
      case c: Conf.Obj =>
        (c.getOption[Symbol.Global]("symbol") |@|
          c.getOption[String]("message") |@|
          c.getOption[String]("id") |@|
          c.getOption[FilterMatcher]("regex"))
          .andThen {
            case (((Some(_), b), c), Some(_)) =>
              Configured.notOk(ConfError.message(
                "Cannot specify both symbol and regex, only one of them is allowed."))
            case (((a @ Some(_), b), c), None) =>
              Configured.ok(DisabledSymbol(a, b.map(normalizeMessage), c, None))
            case (((None, b), c), d @ Some(_)) =>
              Configured.ok(DisabledSymbol(None, b.map(normalizeMessage), c, d))
            case (((None, b), c), None) =>
              Configured.notOk(
                ConfError.message("Either symbol or regex must be specified."))
          }
      case s: Conf.Str =>
        symbolGlobalReader
          .read(s)
          .map(sym => DisabledSymbol(Some(sym), None, None, None))
    }
}

case class UnlessInsideBlock(
    @Description("The symbols that represent 'safe' blocks.")
    safeBlocks: List[DisabledSymbol],
    @Description(
      "The unsafe symbols that are banned unless inside a 'safe' block")
    symbols: List[DisabledSymbol])

object UnlessInsideBlock {
  implicit val surface: Surface[UnlessInsideBlock] =
    generic.deriveSurface[UnlessInsideBlock]
  val safeBlocksReader: ConfDecoder[List[DisabledSymbol]] =
    ConfDecoder.instanceF[List[DisabledSymbol]] { conf =>
      val toRead = conf match {
        case str @ Conf.Str(_) => Conf.Lst(str)
        case e => e
      }
      ConfDecoder[List[DisabledSymbol]].read(toRead)
    }
  // NOTE(olafur): metaconfig.generic.deriveDecoder requires a default base values.
  // Here we require all fields to be provided by the user so we write the decoder manually.
  implicit val reader: ConfDecoder[UnlessInsideBlock] =
    ConfDecoder.instanceF[UnlessInsideBlock] {
      case c: Conf.Obj =>
        (c.get("safeBlocks", "safeBlock")(safeBlocksReader) |@|
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
    @Description("List of symbols to disable if inferred. " +
      "Does not report an error for symbols written explicitly in source code. " +
      "NOTE: Requires the compiler option -P:semanticdb:synthetics:on.")
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
                    |    safeBlocks = [ "scala.util.Try" ]
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
    unlessInside.flatMap(_.safeBlocks)
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
