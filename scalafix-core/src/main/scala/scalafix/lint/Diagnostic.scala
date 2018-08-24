package scalafix.lint

import scala.meta.Position
import scalafix.v0

/** A linter message reporting a code style violation.
  *
  * It's idiomatic to implement a custom class that extends this trait for each
  * unique category of linting messages. For example, if you have an "unused code"
  * linter then you might want to create a <code>class UnusedCode extends Diagnostic</code>
  * class with the appropriate context.
  *
  * Expensive values such as the message and explanation can be computed on-demand.
  *
  * @note for a Diagnostic that is associated with a specific rule use
  *       [[scalafix.lint.RuleDiagnostic]].
  */
trait Diagnostic {

  /** The main message of this diagnostic. */
  def message: String

  /** The source code location where this violation appears, Position.None if not available */
  def position: Position

  /** The severity of this message: error, warning or info */
  def severity: LintSeverity = LintSeverity.Error

  /** String ID for the category of this lint message.
    *
    * A linter diagnostic is keyed by two unique values:
    * - the rule name (which is not available in a Diagnostic
    * - the category ID (this value)
    *
    * The categoryID may be empty, in which case the category of this message will be uniquely
    * defined by the rule name. If a linter rule reports multiple different kinds of diagnostics
    * then it's recommended to provide non-empty categoryID.
    */
  def categoryID: String = ""

  /** An optional detailed explanation of this message. */
  def explanation: String = ""

}

object Diagnostic {

  def apply(
      id: String,
      message: String,
      position: Position,
      explanation: String = "",
      severity: LintSeverity = LintSeverity.Error
  ): Diagnostic = {
    SimpleDiagnostic(message, position, severity, id, explanation)
  }

  /** Construct an eager instance of a Diagnostic. */
  @deprecated(
    "Use Diagnostic(categoryID,message,position,explanation) instead",
    "0.6.0")
  def apply(
      message: String,
      position: Position,
      category: v0.LintCategory): v0.LintMessage = {
    v0.LintMessage(message, position, category)
  }

  /** Basic diagnostic with eagerly computed message and explanation. */
  private final case class SimpleDiagnostic(
      message: String,
      position: Position,
      override val severity: LintSeverity,
      override val categoryID: String,
      override val explanation: String
  ) extends Diagnostic
}
