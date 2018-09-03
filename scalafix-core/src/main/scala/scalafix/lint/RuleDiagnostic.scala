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
    val overriddenSeverity: Option[LintSeverity],
    val blameMessage: Option[String]
) {
  override def toString: String = formattedMessage
  def message: String =
    diagnostic.message + blameMessage.map("\n" + _).getOrElse("")
  def position: Position = diagnostic.position
  def severity: LintSeverity = overriddenSeverity.getOrElse(diagnostic.severity)
  def explanation: String = diagnostic.explanation
  def id: LintID = LintID(rule.value, diagnostic.categoryID)

  def withBlame(msg: String): RuleDiagnostic =
    new RuleDiagnostic(diagnostic, rule, overriddenSeverity, Some(msg))

  /** A pretty-printed representation of this diagnostic without detailed explanation. */
  def formattedMessage: String = {
    val msg0 = new StringBuilder()
      .append("[")
      .append(id.fullID)
      .append("]:")
      .append(if (message.isEmpty || message.startsWith("\n")) "" else " ")
      .append(message)

    val msg = blameMessage.map(msg0.append).getOrElse(msg0).toString()

    position.formatMessage(severity.toString, msg)
  }
}

object RuleDiagnostic {
  def apply(
      diagnostic: Diagnostic,
      rule: RuleName,
      configuredSeverity: Option[LintSeverity]): RuleDiagnostic = {
    new RuleDiagnostic(diagnostic, rule, configuredSeverity, None)
  }
}
