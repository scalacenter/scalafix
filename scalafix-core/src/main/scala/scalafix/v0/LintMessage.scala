package scalafix.v0

import scala.meta.Position
import scalafix.lint.LintSeverity

/** An observation of a LintCategory at a particular position
  *
  * @param message The message to display to the user. If empty, LintID.explanation
  *                is used instead.
  * @param position Optionally place a caret under a location in a source file.
  *                 For an empty position use Position.None.
  * @param category the LintCategory associated with this message.
  */
final case class LintMessage(
    message: String,
    position: Position,
    category: LintCategory
) extends Diagnostic {
  @deprecated("Use format(explain: Boolean) instead", "0.5.4")
  def format(owner: RuleName, explain: Boolean): String =
    format(explain)

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

  override def categoryID: String = category.id
  override def severity: LintSeverity = category.severity
  override def explanation: String = category.explanation

  def withOwner(owner: RuleName): LintMessage =
    copy(category = category.withOwner(owner))
}
