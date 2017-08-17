package scalafix.internal.rewrite

import scala.meta._
import scalafix.Patch
import scalafix.rewrite.Rewrite
import scalafix.rewrite.RewriteCtx

case object ExplicitUnit extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit")
    }.asPatch
  }
}
