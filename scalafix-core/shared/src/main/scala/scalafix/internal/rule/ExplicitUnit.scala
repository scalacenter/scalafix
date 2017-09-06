package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.rule.RuleName

case object ExplicitUnit extends Rule("ExplicitUnit") {
  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit")
    }.asPatch
  }
}
