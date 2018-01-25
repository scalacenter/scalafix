package scalafix.internal.config

import metaconfig.ConfDecoder
import metaconfig.annotation._
import org.langmeta.Symbol
import metaconfig.generic
import metaconfig.generic.Surface

case class NoInferConfig(
    @Description("The list of symbols to must not get inferred.")
    @ExampleValue("""[
                    |  # With custom message (recommended)
                    |  {
                    |    symbol = "scala.Any.AsInstanceOf"
                    |    message = "Use pattern matching instead"
                    |  }
                    |  # Without custom message (discouraged)
                    |  "com.Bar.disabledFunction"
                    |]""".stripMargin)
    symbols: List[Symbol.Global] = Nil
)

object NoInferConfig {
  implicit val surface: Surface[NoInferConfig] =
    generic.deriveSurface[NoInferConfig]
  val default: NoInferConfig = NoInferConfig()
  implicit val decoder: ConfDecoder[NoInferConfig] =
    generic.deriveDecoder[NoInferConfig](default)
}
