package scalafix
package rewrite

import scala.meta._

case object NoXml extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit")
    }.asPatch
  }
}
