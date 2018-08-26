package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token
import scalafix.v1._

class DottyVarArgPattern extends SyntacticRule("DottyVarArgPattern") {
  override def description: String =
    "Rewrite to convert :_* vararg pattern syntax to @ syntax supported in Dotty."
  override def fix(implicit doc: Doc): Patch = {
    val patches = doc.tree.collect {
      case bind @ Pat.Bind(_, Pat.SeqWildcard()) =>
        doc.tokenList
          .leading(bind.tokens.last)
          .collectFirst {
            case tok @ Token.At() =>
              Patch.replaceToken(tok, ":")
          }
          .asPatch
    }
    patches.asPatch
  }

}
