package scalafix.v0

import scalafix.internal.config.LintConfig
import scala.meta.inputs.Position
import scalafix.lint.LintSeverity

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
  private def noExplanation: LintCategory =
    new LintCategory(id, explanation, severity)
  def at(message: String, position: Position): Diagnostic =
    LintMessage(message, position, this)
  def at(message: String): Diagnostic =
    LintMessage(message, Position.None, this)
  def at(position: Position): Diagnostic =
    LintMessage(explanation, position, noExplanation)

  def withOwner(owner: RuleName): LintCategory =
    copy(id = fullId(owner))

  def withConfig(config: LintConfig): LintCategory = {
    val newSeverity =
      config
        .getConfiguredSeverity(id)
        .getOrElse(severity)

    copy(severity = newSeverity)
  }

  def fullId(owner: RuleName): String =
    if (owner.isEmpty) id
    else if (id.isEmpty) owner.value
    else s"${owner.value}.$id"
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
