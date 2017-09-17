package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta._

final case class DisableConfig(disabledSymbols: List[Symbol]) 

object DisableConfig {
  lazy val empty: DisableConfig = DisableConfig(Nil)
}

object DisableConfigDecoder {
  implicit lazy val configDecoder: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig] {
      _.getOrElse("disabledSymbols")(Nil: List[Symbol]) map (DisableConfig(_))
    }
}
