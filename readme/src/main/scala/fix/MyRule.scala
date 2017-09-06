package fix

import scala.meta.Name
import scalafix.Patch
import scalafix.Rule
import scalafix.rule.RuleCtx

object MyRule {
  // Syntactic
  case object Uppercase extends Rule {
    override def fix(ctx: RuleCtx): Patch =
      ctx.tree.collect {
        case tree @ Name(name) => ctx.replaceTree(tree, name.toUpperCase)
      }.asPatch
  }
}
