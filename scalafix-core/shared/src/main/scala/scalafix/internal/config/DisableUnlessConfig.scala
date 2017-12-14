package scalafix.internal.config

import metaconfig.ConfDecoder
import metaconfig.Configured.{NotOk, Ok}
import org.langmeta.Symbol

import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix
import scalafix.internal.util._

sealed trait UnlessMode
case object UnlessInside extends UnlessMode
case object UnlessOutside extends UnlessMode

case class UnlessConfig(mode: UnlessMode,
                        block: Symbol.Global,
                        symbol: Symbol.Global,
                        message: Option[String])

object UnlessConfig {
  implicit val decoder: ConfDecoder[UnlessConfig] =
    ConfDecoder.instanceF[UnlessConfig] { c =>
      (
        (c.get[String]("mode") match {
          case Ok("inside")  => Ok(UnlessInside)
          case Ok("outside") => Ok(UnlessOutside)
          case _             => Ok(UnlessInside)
        }) |@|
          c.get[Symbol.Global]("block") |@|
          c.get[Symbol.Global]("symbol") |@|
          (c.get[String]("message") match {
            case Ok(s)    => Ok(Some(s))
            case NotOk(_) => Ok(None)
          })
        // weird construction, maybe there is a better way?
        // can we add fold[B](ok: T => B)(notOk: => B): Configured[T]?
        ).map { case (((a, b), c), d) => UnlessConfig(a, b, c, d) }
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
