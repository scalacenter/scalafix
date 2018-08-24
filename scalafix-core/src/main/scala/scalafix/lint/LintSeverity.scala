package scalafix.lint

sealed abstract class LintSeverity {
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
