package scalafix.config

@metaconfig.DeriveConfDecoder
case class DebugConfig(
    printSymbols: Boolean = false
)
