package scalafix.lint

import scala.meta.Position
import scalafix.rule.RuleName

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
) {

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

  def withOwner(owner: RuleName): LintMessage =
    copy(category = category.withOwner(owner))
}
