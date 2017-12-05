package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RuleCtx

case object DottyVarArgPattern extends Rule("DottyVarArgPattern") {
  override def description: String =
    "Rewrite to convert :_* vararg pattern syntax to @ syntax supported in Dotty."
  override def fix(ctx: RuleCtx): Patch = {
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
