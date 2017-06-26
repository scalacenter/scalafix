package scalafix.config
import metaconfig._

@DeriveConfDecoder
case class DebugConfig(
    printSymbols: Boolean = false
)
