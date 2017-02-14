package scalafix.config

@metaconfig.ConfigReader
case class DebugConfig(
    printSymbols: Boolean = false
)
