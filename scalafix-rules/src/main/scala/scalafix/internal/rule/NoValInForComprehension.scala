package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._

import scalafix.v1._

class NoValInForComprehension extends SyntacticRule("NoValInForComprehension") {

  override def description: String =
    "Removes deprecated val inside for-comprehension binders"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case v: Enumerator.Val =>
        val valTokens =
          v.tokens.takeWhile(t => t.syntax == "val" || t.is[Whitespace])
        valTokens.map(Patch.removeToken).asPatch.atomic
    }.asPatch
  }

}
