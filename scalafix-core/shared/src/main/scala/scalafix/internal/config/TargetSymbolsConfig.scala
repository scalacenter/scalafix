package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta._

sealed trait TargetSymbolsConfig {
  val symbols: List[Symbol]
}

object TargetSymbolsConfig {

  def apply(xs: List[Symbol]): TargetSymbolsConfig =
    new TargetSymbolsConfig { val symbols = xs }

  def empty: TargetSymbolsConfig =
    apply(Nil)

  def decoder: ConfDecoder[TargetSymbolsConfig] =
    ConfDecoder.instanceF[TargetSymbolsConfig] {
      _.getOrElse[List[Symbol.Global]]("symbols")(Nil) map apply
    }
}
