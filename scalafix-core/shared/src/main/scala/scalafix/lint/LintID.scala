package scalafix.lint

import scalafix.rewrite.RewriteName
import scala.meta.inputs.Position

/** A unique identifier for one kind of a linter message.
  *
  * @param id a string ID for this message, typically the name of the
  *           assigned variable.
  * @param explanation An optional explanation for this kind of message.
  * @param severity The default category this message should get reported to.
  *                 Note that users can configure/override the default category.
  */
final case class LintID(
    id: String,
    explanation: String,
    severity: LintSeverity
) {
  def key(owner: RewriteName): String =
    if (owner.isEmpty) id
    else s"$owner.$id"
  private def noExplanation: LintID =
    new LintID(id, explanation, severity)
  def at(message: String, position: Position): LintMessage =
    LintMessage(message, position, this)
  def at(message: String): LintMessage =
    LintMessage(message, Position.None, this)
  def at(position: Position): LintMessage =
    LintMessage(explanation, position, noExplanation)
}

object LintID {
  def error(explain: String)(implicit alias: sourcecode.Name): LintID =
    new LintID(alias.value, explain, LintSeverity.Error)
  def warning(explain: String)(
      implicit alias: sourcecode.Name,
      rewrite: RewriteName): LintID =
    new LintID(alias.value, explain, LintSeverity.Warning)
}
