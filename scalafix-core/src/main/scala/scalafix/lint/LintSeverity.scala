package scalafix.lint

import scalafix.internal.util.Severity

sealed abstract class LintSeverity {
  private[scalafix] def toSeverity: Severity = this match {
    case LintSeverity.Error => Severity.Error
    case LintSeverity.Warning => Severity.Warn
    case LintSeverity.Info => Severity.Info
  }
  def isError: Boolean = this == LintSeverity.Error
  override def toString: String = this match {
    case LintSeverity.Error => "error"
    case LintSeverity.Warning => "warning"
    case LintSeverity.Info => "info"
  }
}

object LintSeverity {
  case object Info extends LintSeverity
  case object Warning extends LintSeverity
  case object Error extends LintSeverity
}
