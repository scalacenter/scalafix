package scalafix.internal.config

import metaconfig.ConfDecoder
import metaconfig.annotation._
import scala.meta.Symbol
import metaconfig.generic
import metaconfig.generic.Surface

case class NoInferConfig(
    @Description("The list of symbols to must not get inferred.")
    @ExampleValue("""[
                    |  "scala.Predef.any2stringadd"
                    |]""".stripMargin)
    symbols: List[Symbol.Global] = Nil
)

object NoInferConfig {
  lazy val badSymbols: List[Symbol] = List(
    Symbol("_root_.java.io.Serializable."),
    Symbol("_root_.scala.Any."),
    Symbol("_root_.scala.AnyVal."),
    Symbol("_root_.scala.Product.")
  )
  implicit val surface: Surface[NoInferConfig] =
    generic.deriveSurface[NoInferConfig]
  val default: NoInferConfig = NoInferConfig(badSymbols.collect {
    case s: Symbol.Global => s
  })
  implicit val decoder: ConfDecoder[NoInferConfig] =
    generic.deriveDecoder[NoInferConfig](default)
}
