package scalafix.internal.config

object MetaconfigParser {
  implicit val parser = metaconfig.hocon.hoconMetaconfigParser
}
