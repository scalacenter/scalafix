package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RewriteCtx
import scalafix.rule.RewriteName

case object ExplicitUnit extends Rule {
  override def name: RewriteName = "ExplicitUnit"
  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit")
    }.asPatch
  }
}
