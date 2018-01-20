package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scalafix.internal.config.MissingFinalConfig
import scalafix.lint.LintCategory
import scalafix.patch.Patch
import scalafix.rule.{Rule, RuleCtx}
import scalafix.{SemanticRule, SemanticdbIndex}

final case class MissingFinal(
    index: SemanticdbIndex,
    config: MissingFinalConfig)
    extends SemanticRule(
      index,
      "MissingFinal"
    ) {

  override def description: String =
    "Rule that checks for or adds final modifier in corresponding places"

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("missingFinal", "MissingFinal")(MissingFinalConfig.default)
      .map(MissingFinal(index, _))

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
        if (config.finalCaseClass) {
          addFinal(mods, t)
        } else {
          ctx.lint(
            error
              .copy(id = "case class")
              .at("Case class should have final modifier", t.pos)
          )
        }
      case t @ Defn.Class(mods, _, _, _, templ)
          if leaksSealedParent(mods, templ) =>
        if (config.finalClass) {
          addFinal(mods, t)
        } else {
          ctx.lint(
            error
              .copy(id = "class")
              .at(
                "Class that extends sealed parent should have final modifier",
                t.pos)
          )
        }
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
