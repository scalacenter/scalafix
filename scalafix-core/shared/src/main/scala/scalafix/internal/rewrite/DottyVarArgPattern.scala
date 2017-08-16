package scalafix.internal.rewrite

import scala.meta._
import scala.meta.tokens.Token
import scalafix.Patch
import scalafix.rewrite.Rewrite
import scalafix.rewrite.RewriteCtx

case object DottyVarArgPattern extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
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
