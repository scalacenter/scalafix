package scalafix.config

object MetaconfigParser {
  implicit val parser = metaconfig.hocon.hoconMetaconfigParser
}
