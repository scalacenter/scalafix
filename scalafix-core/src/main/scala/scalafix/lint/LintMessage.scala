package scalafix.lint

import scala.meta.Position
import scalafix.rule.RuleName

/** A linter message reporting a code style violation.
  *
  * It's idiomatic to implement a custom class that extends this trait for each
  * unique category of linting messages. For example, if you have an "unused code"
  * linter then you might want to create a <code>class UnusedCode extends LintMessage</code>
  * class with the appropriate context.
  *
  * Expensive values such as the message and explanation can be computed on-demand.
  *
  * @note for a LintMessage that is associated with a specific rule use
  *       [[scalafix.lint.LintDiagnostic]].
  */
trait LintMessage {

  /** The main message of this diagnostic. */
  def message: String

  /** The source code location where this violation appears, Position.None if not available */
  def position: Position

  /** The severity of this message: error, warning or info */
  def severity: LintSeverity = LintSeverity.Error

  /** String ID for the category of this lint message.
    *
    * A linter diagnostic is keyed by two unique values:
    * - the rule name (which is not available in a LintMessage
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

object LintMessage {

  /** Construct an eager instance of a LintMessage. */
  def apply(
      message: String,
      position: Position,
      category: LintCategory): EagerLintMessage = {
    EagerLintMessage(message, position, category)
  }

  /** An observation of a LintCategory at a particular position.
    *
    * @param message The message to display to the user. If empty, LintID.explanation
    *                is used instead.
    * @param position Optionally place a caret under a location in a source file.
    *                 For an empty position use Position.None.
    * @param category the LintCategory associated with this message.
    */
  final case class EagerLintMessage(
      message: String,
      position: Position,
      category: LintCategory
  ) extends LintMessage {
    def format(explain: Boolean): String = {
      val explanation =
        if (explain)
          s"""
             |Explanation:
             |${category.explanation}
             |""".stripMargin
        else ""

      s"[${category.id}] $message$explanation"
    }

    def id: String = category.id

    def withOwner(owner: RuleName): EagerLintMessage =
      copy(category = category.withOwner(owner))

    override def severity: LintSeverity = category.severity
    override def categoryID: String = category.id
    override def explanation: String = category.explanation
  }
}
