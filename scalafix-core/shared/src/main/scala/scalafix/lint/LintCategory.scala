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
final case class LintCategory(
    id: String,
    explanation: String,
    severity: LintSeverity
) {
  def key(owner: RewriteName): String =
    if (owner.isEmpty) id
    else s"$owner.$id"
  private def noExplanation: LintCategory =
    new LintCategory(id, explanation, severity)
  def at(message: String, position: Position): LintMessage =
    LintMessage(message, position, this)
  def at(message: String): LintMessage =
    LintMessage(message, Position.None, this)
  def at(position: Position): LintMessage =
    LintMessage(explanation, position, noExplanation)
}

object LintCategory {
  def error(explain: String)(implicit alias: sourcecode.Name): LintCategory =
    new LintCategory(alias.value, explain, LintSeverity.Error)
  def warning(explain: String)(
      implicit alias: sourcecode.Name,
      rewrite: RewriteName): LintCategory =
    new LintCategory(alias.value, explain, LintSeverity.Warning)
}
