package scalafix.lint

import scalafix.rewrite.RewriteName
import scala.meta.inputs.Position

/** A unique identifier for one kind of a linter message.
  *
  * @param id a string ID for this message, typically the name of the
  *           assigned variable.
  * @param owner The rewrite that owns this lint message, to distinguish
  *              lint messages with conflicting IDs from different rewrites.
  * @param explanation An optional explanation for this kind of message.
  * @param category The default category this message should get reported to.
  *                 Note that users can configure/override the default category.
  */
final class LintID(
    val id: String,
    val owner: RewriteName,
    val explanation: String,
    val category: LintCategory
) {
  override def toString: String = key
  lazy val key = s"$owner.$id"
  private def noExplanation: LintID =
    new LintID(id, owner, explanation, category)
  def at(message: String, position: Position): LintMessage =
    new LintMessage(message, position, this)
  def at(message: String): LintMessage =
    new LintMessage(message, Position.None, this)
  def at(position: Position): LintMessage =
    new LintMessage(explanation, position, noExplanation)
}

object LintID {
  def error(explain: String)(
      implicit alias: sourcecode.Name,
      rewrite: RewriteName): LintID =
    new LintID(alias.value, rewrite, explain, LintCategory.Error)
  def warning(explain: String)(
      implicit alias: sourcecode.Name,
      rewrite: RewriteName): LintID =
    new LintID(alias.value, rewrite, explain, LintCategory.Warning)
}
