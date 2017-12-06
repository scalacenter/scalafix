package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._

import metaconfig.{Conf, Configured}
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory

case object NoFinalize extends Rule("NoFinalize") {
  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case defn: Defn.Def if isFinalized(defn) => error(defn.name.pos)
    }
  }

  private val errorCategory: LintCategory =
    LintCategory.error(
      "finalizer may be never called and have a severe performance penalty")

  private def error(pos: Position): LintMessage = errorCategory.at(pos)

  private def isFinalized(defn: Defn.Def): Boolean = {
    defn match {
      case q"override def finalize(): Unit = $_" => true
      case q"override protected def finalize(): Unit = $_" => true
      case q"override protected def finalize() = $_" => true
      case _ => false
    }
  }
}
