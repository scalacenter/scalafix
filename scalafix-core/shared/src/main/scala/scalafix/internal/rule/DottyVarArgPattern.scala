package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RewriteCtx

case object DottyVarArgPattern extends Rule {
  def name = "DottyVarArgPattern"
  override def fix(ctx: RewriteCtx): Patch = {
    val patches = ctx.tree.collect {
      case bind @ Pat.Bind(_, Pat.SeqWildcard()) =>
        ctx.tokenList
          .leading(bind.tokens.last)
          .collectFirst {
            case tok @ Token.At() =>
              ctx.replaceToken(tok, ":")
          }
          .asPatch
    }
    patches.asPatch
  }

}
