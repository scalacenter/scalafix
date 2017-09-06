package scalafix.internal.rewrite

import scala.meta._
import scalafix.Patch
import scalafix.rewrite.Rule
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.RewriteName

case object ExplicitUnit extends Rule {
  override def name: RewriteName = "ExplicitUnit"
  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit")
    }.asPatch
  }
}
