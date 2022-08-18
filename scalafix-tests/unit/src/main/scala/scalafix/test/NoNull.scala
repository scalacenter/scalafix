package scalafix.test

import scala.meta.Lit
import scala.meta.XtensionCollectionLikeUI

import scalafix.lint.Diagnostic
import scalafix.v0.LintCategory
import scalafix.v0.Rule
import scalafix.v0.RuleCtx

object NoNull extends Rule("NoNull") {
  val error: LintCategory = LintCategory.error("Nulls are not allowed.")

  override def check(ctx: RuleCtx): List[Diagnostic] = ctx.tree.collect {
    case nil @ Lit.Null() => error.at(nil.pos)
  }
}
