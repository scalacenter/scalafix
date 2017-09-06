package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RewriteCtx

case object NoValInForComprehension extends Rule {
  def name = "NoValInForComprehension"

  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tree.collect {
      case v: Enumerator.Val =>
        val valTokens =
          v.tokens.takeWhile(t => t.syntax == "val" || t.is[Whitespace])
        valTokens.map(ctx.removeToken).asPatch
    }.asPatch
  }

}
