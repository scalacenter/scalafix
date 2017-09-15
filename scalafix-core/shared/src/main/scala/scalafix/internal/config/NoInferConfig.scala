package scalafix.internal.config

import metaconfig._
import org.langmeta._

case class NoInferConfig(
    extraSymbols: List[Symbol] = Nil
) {
  implicit val reader: ConfDecoder[NoInferConfig] =
    ConfDecoder.instanceF[NoInferConfig] { c =>
        c.getOrElse("extraSymbols")(extraSymbols).map(xs => NoInferConfig(xs))
    }
  implicit val symbolReader: ConfDecoder[Symbol] =
    ConfDecoder.instanceF[Symbol] { c =>
      c.as[String].map(Symbol.apply)
    }
}

object NoInferConfig {
  val default = NoInferConfig()
  implicit val reader: ConfDecoder[NoInferConfig] = default.reader
}