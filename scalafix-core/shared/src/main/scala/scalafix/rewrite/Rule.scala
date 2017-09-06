package scalafix.rewrite

import scalafix.patch.Patch
import scalafix.rule.SemanticRule
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.rule.RuleName
import scalafix.util.SemanticCtx

@deprecated("Moved to scalafix.rule.Rule", "0.5.0")
abstract class Rewrite extends Rule {
  override def name: RuleName = this.getClass.getSimpleName
  override def fix(ctx: RuleCtx): Patch = rewrite(ctx)
  def rewrite(ctx: RuleCtx): Patch
}

@deprecated("Moved to scalafix.rule.SemanticRule", "0.5.0")
abstract class SemanticRewrite(sctx: SemanticCtx) extends SemanticRule(sctx) {
  override def name: RuleName = this.getClass.getSimpleName
  override def fix(ctx: RuleCtx): Patch = rewrite(ctx)
  def rewrite(ctx: RuleCtx): Patch
}
