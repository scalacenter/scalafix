package scalafix.internal.rule

import scala.meta._

import scalafix.{Patch, SemanticRule, SemanticdbIndex}
import scalafix.rule.{RuleCtx, RuleName}
import scalafix.util.SymbolMatcher

final case class NoSomeConstructor(index: SemanticdbIndex)
    extends SemanticRule(index, RuleName("NoSomeConstructor")) {

  override def description: String =
    "Rewrite that replaces Some() constructor by Option() constructor"

  override def fix(ctx: RuleCtx): Patch = {
    val someConstructor = SymbolMatcher.normalized(
      Symbol("_root_.scala.Some.")
    )

    ctx.tree.collect {
      case Term.Apply(someConstructor(fun), _) =>
        ctx.replaceTree(fun, "Option").atomic
    }.asPatch
  }
}
