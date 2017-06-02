package scalafix.rewrite

import scala.meta._

import scala.meta.tokens.Token
import scalafix.Patch

case object DottyVarArgPattern extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
    val patches = ctx.tree.collect {
      case bind @ Pat.Bind(_, Pat.Arg.SeqWildcard()) =>
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
