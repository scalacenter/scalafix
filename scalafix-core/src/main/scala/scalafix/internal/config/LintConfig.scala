package scalafix.internal.config

import metaconfig.ConfDecoder
import metaconfig.generic
import metaconfig.generic.Surface
import scalafix.internal.util.MetaconfigCompatMacros
import scalafix.lint.LintSeverity

case class LintConfig(
    explain: Boolean = false,
    ignore: FilterMatcher = FilterMatcher.matchNothing,
    info: FilterMatcher = FilterMatcher.matchNothing,
    warning: FilterMatcher = FilterMatcher.matchNothing,
    error: FilterMatcher = FilterMatcher.matchNothing
) {
  def getConfiguredSeverity(key: String): Option[LintSeverity] =
    Option(key).collect {
      case error() => LintSeverity.Error
      case warning() => LintSeverity.Warning
      case info() => LintSeverity.Info
    }
}

object LintConfig {
  implicit val surface: Surface[LintConfig] =
    MetaconfigCompatMacros.deriveSurfaceOrig[LintConfig]
  lazy val default: LintConfig = LintConfig()
  implicit val decoder: ConfDecoder[LintConfig] =
    generic.deriveDecoder[LintConfig](default)
}
