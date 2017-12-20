package scalafix.internal.config

object MetaconfigParser {
  implicit val parser: metaconfig.MetaconfigParser =
    metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
}
