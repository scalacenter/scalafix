package scalafix.lint

import scala.meta.Position
import scalafix.internal.util.PositionSyntax._
import scalafix.rule.RuleName

/**
  * A linter messages that has been associated with a rule.
  *
  * @note The difference between RuleDiagnostic and Diagnostic is that
  *       Diagnostic is not associated with a rule while RuleDiagnostic
  *       interfaces matches closely the ScalafixDiagnostic interface
  *       from the scalafix-interfaces Java-only module.
  */
trait RuleDiagnostic {

  /** The main message of this diagnostic. */
  def message: String

  /** The source code location where this violation appears, Position.None if not available. */
  def position: Position

  /** The severity of this message: error, warning or info. */
  def severity: LintSeverity

  /** An optional detailed explanation of this message. */
  def explanation: String

  /** A unique identifier for the category of this lint diagnostic. */
  def id: LintID

  /** A pretty-printed representation of this diagnostic without detailed explanation. */
  def formattedMessage: String = {
    val msg = new StringBuilder()
      .append("[")
      .append(id.fullStringID)
      .append("]:")
      .append(if (message.isEmpty || message.startsWith("\n")) "" else " ")
      .append(message)
      .toString()
    position.formatMessage(severity.toString, msg)
  }
}

object RuleDiagnostic {
  def apply(
      message: Diagnostic,
      rule: RuleName,
      configuredSeverity: Option[LintSeverity]): RuleDiagnostic =
    new LazyRuleDiagnostic(message, rule, configuredSeverity)

  final class LazyRuleDiagnostic(
      val lintMessage: Diagnostic,
      val rule: RuleName,
      val configuredSeverity: Option[LintSeverity]
  ) extends RuleDiagnostic {
    override def toString: String = formattedMessage
    override def message: String = lintMessage.message
    override def position: Position = lintMessage.position
    override def severity: LintSeverity =
      configuredSeverity.getOrElse(lintMessage.severity)
    override def explanation: String = lintMessage.explanation
    override def id: LintID = LintID(rule.value, lintMessage.categoryID)
  }

}
