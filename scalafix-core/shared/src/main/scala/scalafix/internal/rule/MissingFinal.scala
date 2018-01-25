package scalafix.internal.rule

import scala.meta._
import scalafix.lint.LintCategory
import scalafix.patch.Patch
import scalafix.rule.RuleCtx
import scalafix.{SemanticRule, SemanticdbIndex}

final case class MissingFinal(index: SemanticdbIndex)
    extends SemanticRule(
      index,
      "MissingFinal"
    ) {

  override def description: String =
    "Rule that checks for or adds final modifier in the corresponding places"

  private lazy val error: LintCategory =
    LintCategory.error(
      "Some constructions should have final modifier"
    )

  override def fix(ctx: RuleCtx): Patch = {
    def isSealed(tpe: Type): Boolean =
      ctx.index.denotation(tpe).exists(_.isSealed)

    def leaksSealedParent(mods: List[Mod], templ: Template): Boolean =
      templ.inits.exists(i => isSealed(i.tpe)) &&
        mods.forall(m => !m.is[Mod.Final] && !m.is[Mod.Sealed])

    def addFinal(mods: Seq[Mod], d: Defn): Patch =
      mods.find(!_.is[Mod.Annot]) match {
        case Some(mod) => ctx.addLeft(mod, "final ")
        case None =>
          mods.lastOption match {
            case Some(lastMod) => ctx.addRight(lastMod, " final")
            case None => ctx.addLeft(d, "final ")
          }
      }

    ctx.tree.collect {
      case t @ Defn.Class(mods, _, _, _, _)
          if mods.exists(_.is[Mod.Case]) &&
            !mods.exists(_.is[Mod.Final]) =>
        addFinal(mods, t)
      case t @ Defn.Class(mods, _, _, _, templ)
          if leaksSealedParent(mods, templ) =>
        ctx.lint(
          error
            .copy(id = "class")
            .at("Class extends sealed parent", t.pos)
        )
      case t @ Defn.Trait(mods, _, _, _, templ)
          if leaksSealedParent(mods, templ) =>
        ctx.lint(
          error
            .copy(id = "trait")
            .at("Trait extends sealed parent", t.pos)
        )
    }.asPatch
  }
}
