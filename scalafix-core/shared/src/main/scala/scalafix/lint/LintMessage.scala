package scalafix.lint

import scala.meta.Position
import scalafix.internal.util.Severity
import scalafix.rewrite.RewriteName

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

class LintMessage(
    val message: String,
    val position: Position,
    val id: LintID
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

sealed abstract class LintCategory {
  private[scalafix] def toSeverity: Severity = this match {
    case LintCategory.Error => Severity.Error
    case LintCategory.Warning => Severity.Warn
  }
  def isError: Boolean = this == LintCategory.Error

  override def toString: String = this match {
    case LintCategory.Error => "error"
    case LintCategory.Warning => "warning"
  }
}
object LintCategory {
  case object Warning extends LintCategory
  case object Error extends LintCategory
}
