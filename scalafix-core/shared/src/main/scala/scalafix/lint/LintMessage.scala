package scalafix.lint

import scala.meta.Position

/** An instance of a LintID with a custom message at a particular position
  *
  * @param message The message to display to the user. If empty, LintID.explanation
  *                is used instead.
  * @param position Optionally place a caret under a location in a source file.
  *                 For an empty position use Position.None.
  * @param id the LintID associated with this message.
  */
final case class LintMessage(
    message: String,
    position: Position,
    id: LintID
) {
  def format(explain: Boolean): String = {
    val explanation =
      if (explain)
        s"""
           |Explanation:
           |${id.explanation}
           |""".stripMargin
      else ""
    s"[${id.owner.name}.${id.id}] $message$explanation"
  }
}
