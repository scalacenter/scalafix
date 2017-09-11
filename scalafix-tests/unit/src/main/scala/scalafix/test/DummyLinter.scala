package scalafix.test

import scalafix.LintCategory
import scalafix.LintMessage
import scalafix.Rule
import scalafix.RuleCtx

object DummyLinter extends Rule("DummyLinter") {
  val error = LintCategory.error("Bam!")
  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    error.at(ctx.tokenList.prev(ctx.tokens.last).pos) :: Nil
  }
}
