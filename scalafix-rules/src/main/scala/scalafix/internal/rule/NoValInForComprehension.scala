package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scalafix.v1._

class NoValInForComprehension extends SyntacticRule("NoValInForComprehension") {

  override def description: String =
    "Rewrite that removes redundant val inside for-comprehensions"

  override def fix(implicit doc: Doc): Patch = {
    doc.tree.collect {
      case v: Enumerator.Val =>
        val valTokens =
          v.tokens.takeWhile(t => t.syntax == "val" || t.is[Whitespace])
        valTokens.map(Patch.removeToken).asPatch.atomic
    }.asPatch
  }

}
