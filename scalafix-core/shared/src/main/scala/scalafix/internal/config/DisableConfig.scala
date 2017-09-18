package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta._

final case class DisableConfig(symbols: List[Symbol])

object DisableConfig {
  lazy val empty: DisableConfig = DisableConfig(Nil)
}

object DisableConfigDecoder {
  implicit lazy val configDecoder: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig] {
      _.getOrElse[List[Symbol.Global]]("symbols")(Nil).map(DisableConfig.apply)
    }
}
