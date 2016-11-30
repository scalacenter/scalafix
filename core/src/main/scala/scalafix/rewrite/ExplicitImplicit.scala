package scalafix.rewrite

import scala.meta._
import scalafix.util.Patch

case object ExplicitImplicit extends Rewrite {
  override def rewrite(ast: Tree, ctx: RewriteCtx): Seq[Patch] = {
    require(ctx.semantic.nonEmpty)
    Nil
  }
}
