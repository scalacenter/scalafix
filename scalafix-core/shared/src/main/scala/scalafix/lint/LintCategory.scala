package scalafix.lint

import scalafix.internal.util.Severity

sealed abstract class LintCategory {
  private[scalafix] def toSeverity: Severity = this match {
    case LintCategory.Error => Severity.Error
    case LintCategory.Warning => Severity.Warn
    case LintCategory.Info => Severity.Info
  }
  def isError: Boolean = this == LintCategory.Error
  override def toString: String = this match {
    case LintCategory.Error => "error"
    case LintCategory.Warning => "warning"
    case LintCategory.Info => "info"
  }
}

object LintCategory {
  case object Info extends LintCategory
  case object Warning extends LintCategory
  case object Error extends LintCategory
}