package scalafix.test

import scalafix.LintCategory
import scalafix.LintMessage
import scalafix.Rule
import scalafix.RuleCtx

import scala.meta._

object EscapeHatchDummyLinter extends Rule("EscapeHatchDummyLinter") {
  val error = LintCategory.error("Dummy!")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case tree @ Name(name) if name.contains("Dummy") => error.at(tree.pos)
    }
  }
}

object EscapeHatchNoNulls extends Rule("EscapeHatchNoNulls") {
  val error = LintCategory.error("Nulls are not allowed.")
  override def check(ctx: RuleCtx): List[LintMessage] = ctx.tree.collect {
    case nil @ q"null" => error.at(nil.pos)
  }
}