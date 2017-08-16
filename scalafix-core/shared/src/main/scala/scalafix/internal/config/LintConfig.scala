package scalafix.internal.config

import metaconfig.ConfDecoder

case class LintConfig(
    explain: Boolean = false,
    ignore: FilterMatcher = FilterMatcher.matchNothing,
    warning: FilterMatcher = FilterMatcher.matchNothing,
    error: FilterMatcher = FilterMatcher.matchNothing
) {
  val reader: ConfDecoder[LintConfig] =
    ConfDecoder.instanceF[LintConfig] { c =>
      (
        c.getOrElse("explain")(explain) |@|
          c.getOrElse("ignore")(ignore) |@|
          c.getOrElse("warning")(warning) |@|
          c.getOrElse("error")(error)
      ).map { case (((a, b), c), d) => LintConfig(a, b, c, d) }
    }
}

object LintConfig {
  lazy val default = LintConfig()
}
