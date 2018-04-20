package scalafix.internal.rule

import scala.meta._
import scala.meta.transversers.Traverser
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
      (mods.find(!_.is[Mod.Annot]) match {
        case Some(mod) => ctx.addLeft(mod, "final ")
        case None =>
          mods.lastOption match {
            case Some(lastMod) => ctx.addRight(lastMod, " final")
            case None => ctx.addLeft(d, "final ")
          }
      }).atomic

    def isNonFinalConcreteCaseClass(mods: List[Mod]) =
      mods.exists(_.is[Mod.Case]) &&
        !mods.exists(m => m.is[Mod.Final] || m.is[Mod.Abstract])

    collect(ctx.tree) {
      case (t @ Defn.Class(mods, _, _, _, _), parentPropagatesOuterRef)
          if isNonFinalConcreteCaseClass(mods) && !parentPropagatesOuterRef =>
        (Some(addFinal(mods, t)), PropagatesOuterRef)
      case (t @ Defn.Class(mods, _, _, _, templ), _)
          if leaksSealedParent(mods, templ) =>
        val lint = ctx.lint(
          error
            .copy(id = "class")
            .at("Class extends sealed parent", t.pos))
        (Some(lint), PropagatesOuterRef)
      case (t @ Defn.Trait(mods, _, _, _, templ), _)
          if leaksSealedParent(mods, templ) =>
        val lint = ctx.lint(
          error
            .copy(id = "trait")
            .at("Trait extends sealed parent", t.pos))
        (Some(lint), PropagatesOuterRef)
      case (_: Defn.Class | _: Defn.Trait, _) => (None, PropagatesOuterRef)
      case (_: Defn.Object | _: Template, outerRef) => (None, outerRef)
      case _ => (None, NoOuterRef)
    }.asPatch
  }

  private val PropagatesOuterRef = true
  private val NoOuterRef = false

  private def collect(tree: Tree)(
      fn: (Tree, Boolean) => (Option[Patch], Boolean)): List[Patch] = {
    val buf = scala.collection.mutable.ListBuffer[Patch]()
    var outerRef = NoOuterRef

    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        val prev = outerRef
        fn(tree, prev) match {
          case (Some(patch), outer) =>
            buf += patch
            outerRef = outer
          case (None, outer) =>
            outerRef = outer
        }
        super.apply(tree)
        outerRef = prev
      }
    }
    traverser(tree)
    buf.toList
  }
}
