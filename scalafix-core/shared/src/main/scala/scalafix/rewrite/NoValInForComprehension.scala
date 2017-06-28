package scalafix
package rewrite

import scala.meta._
import scala.meta.contrib._

case object NoValInForComprehension extends Rewrite {

  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case v: Enumerator.Val =>
        val valTokens =
          v.tokens.takeWhile(t => t.syntax == "val" || t.is[Whitespace])
        valTokens.map(ctx.removeToken).asPatch
    }.asPatch
  }

}
