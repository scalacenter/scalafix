package scalafix.lint

import scalafix.rule.RuleName
import scala.meta.inputs.Position

/** A unique identifier for one kind of a linter message.
  *
  * @param id a string ID for this message, typically the name of the
  *           assigned variable. If id is empty, then the name of the
  *           rewrite reporting this LintCategory is used as id.
  * @param explanation An optional explanation for this kind of message.
  * @param severity The default category this message should get reported to.
  *                 Note that users can configure/override the default category.
  */
final case class LintCategory(
    id: String,
    explanation: String,
    severity: LintSeverity
) {
  def key(owner: RuleName): String =
    if (owner.isEmpty) id
    else if (id.isEmpty) owner.value
    else s"${owner.value}.$id"
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
  def error(id: String, explain: String): LintCategory =
    new LintCategory(id, explain, LintSeverity.Error)
  def error(explain: String): LintCategory =
    new LintCategory("", explain, LintSeverity.Error)
  def warning(id: String, explain: String): LintCategory =
    new LintCategory(id, explain, LintSeverity.Warning)
  def warning(explain: String): LintCategory =
    new LintCategory("", explain, LintSeverity.Warning)
}
