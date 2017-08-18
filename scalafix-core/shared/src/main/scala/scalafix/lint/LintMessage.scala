package scalafix.lint

import scala.meta.Position
import scalafix.rewrite.RewriteName

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
  def format(owner: RewriteName, explain: Boolean): String = {
    val explanation =
      if (explain)
        s"""
           |Explanation:
           |${category.explanation}
           |""".stripMargin
      else ""
    s"[${owner.name}.${category.id}] $message$explanation"
  }
}
