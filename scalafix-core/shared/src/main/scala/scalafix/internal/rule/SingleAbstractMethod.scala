package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

case class SingleAbstractMethod(index: SemanticdbIndex) extends SemanticRule(
  index,
  "SingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = ???
}
