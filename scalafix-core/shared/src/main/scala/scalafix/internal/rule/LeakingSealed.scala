package scalafix.internal.rule

import scala.meta._
import scalafix.lint.LintCategory
import scalafix.rule.RuleCtx
import scalafix.{LintMessage, SemanticRule, SemanticdbIndex}

final case class LeakingSealed(index: SemanticdbIndex)
    extends SemanticRule(
      index,
      "LeakingSealed"
    ) {

  override def description: String =
    "Linter that ensures that descendants of a sealed types are final or sealed"

  private lazy val error: LintCategory =
    LintCategory.error(
      """Descendants of a sealed type must be final or sealed. Otherwise this
        |type can be extended in another file through its descendant.""".stripMargin
    )

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    def isSealed(tpe: Type): Boolean =
      ctx.index.denotation(tpe).exists(_.isSealed)

    ctx.tree.collect {
      case t @ Defn.Class(mods, _, _, _, templ)
          if templ.inits.exists(i => isSealed(i.tpe)) &&
            mods.forall(m => !m.is[Mod.Final] && !m.is[Mod.Sealed]) =>
        error.at("Not final/sealed class", t.pos)
      case t @ Defn.Trait(mods, _, _, _, templ)
          if templ.inits.exists(i => isSealed(i.tpe)) &&
            mods.forall(m => !m.is[Mod.Final] && !m.is[Mod.Sealed]) =>
        error.at("Not final/sealed trait", t.pos)
    }
  }
}
