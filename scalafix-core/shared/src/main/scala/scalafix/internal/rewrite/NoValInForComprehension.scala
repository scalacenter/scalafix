package scalafix.internal.rewrite

import scala.meta._
import scala.meta.contrib._
import scalafix.Patch
import scalafix.rewrite.Rule
import scalafix.rewrite.RewriteCtx

case object NoValInForComprehension extends Rule {

  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case v: Enumerator.Val =>
        val valTokens =
          v.tokens.takeWhile(t => t.syntax == "val" || t.is[Whitespace])
        valTokens.map(ctx.removeToken).asPatch
    }.asPatch
  }

}
