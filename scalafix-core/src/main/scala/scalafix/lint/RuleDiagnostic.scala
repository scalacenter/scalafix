package scalafix.lint

import scala.meta.Position
import scalafix.internal.util.PositionSyntax._
import scalafix.rule.RuleName

/**
  * A diagnostic that has been associated with a rule.
  *
  * @param overriddenSeverity Optional .scalafix.conf configuration where user has overriden the severity of
  *                           diagnostics for this rule + category ID.
  */
final class RuleDiagnostic private (
    val diagnostic: Diagnostic,
    val rule: RuleName,
    val overriddenSeverity: Option[LintSeverity]
) {
  override def toString: String = formattedMessage
  def message: String = diagnostic.message
  def position: Position = diagnostic.position
  def severity: LintSeverity = overriddenSeverity.getOrElse(diagnostic.severity)
  def explanation: String = diagnostic.explanation
  def id: LintID = LintID(rule.value, diagnostic.categoryID)

  /** A pretty-printed representation of this diagnostic without detailed explanation. */
  def formattedMessage: String = {
    val msg = new StringBuilder()
      .append("[")
      .append(id.fullID)
      .append("]:")
      .append(if (message.isEmpty || message.startsWith("\n")) "" else " ")
      .append(message)
      .toString()
    position.formatMessage(severity.toString, msg)
  }
}

object RuleDiagnostic {
  def apply(
      diagnostic: Diagnostic,
      rule: RuleName,
      configuredSeverity: Option[LintSeverity]): RuleDiagnostic = {
    new RuleDiagnostic(diagnostic, rule, configuredSeverity)
  }
}
