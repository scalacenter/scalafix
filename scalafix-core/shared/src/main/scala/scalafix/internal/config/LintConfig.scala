package scalafix.internal.config

import scalafix.lint.LintCategory
import scalafix.lint.LintID
import metaconfig.ConfDecoder

case class LintConfig(
    explain: Boolean = false,
    ignore: FilterMatcher = FilterMatcher.matchNothing,
    info: FilterMatcher = FilterMatcher.matchNothing,
    warning: FilterMatcher = FilterMatcher.matchNothing,
    error: FilterMatcher = FilterMatcher.matchNothing
) {
  val reader: ConfDecoder[LintConfig] =
    ConfDecoder.instanceF[LintConfig] { c =>
      (
        c.getOrElse("explain")(explain) |@|
          c.getOrElse("ignore")(ignore) |@|
          c.getOrElse("info")(info) |@|
          c.getOrElse("warning")(warning) |@|
          c.getOrElse("error")(error)
      ).map { case ((((a, b), c), d), e) => LintConfig(a, b, c, d, e) }
    }

  def getConfiguredCategory(key: String): Option[LintCategory] =
    Option(key).collect {
      case error() => LintCategory.Error
      case warning() => LintCategory.Warning
      case info() => LintCategory.Info
    }
}

object LintConfig {
  lazy val default = LintConfig()
}
