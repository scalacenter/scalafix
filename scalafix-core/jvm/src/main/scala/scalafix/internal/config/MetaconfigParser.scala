package scalafix.internal.config

object MetaconfigParser {
  implicit val parser =
    metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
}
