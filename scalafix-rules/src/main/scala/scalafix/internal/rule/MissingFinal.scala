package scalafix.internal.rule

import scala.meta._
import scalafix.v1._

class MissingFinal extends SemanticRule("MissingFinal") {

  override def description: String =
    "Rule that checks for or adds final modifier in the corresponding places"

  override def fix(implicit sdoc: SemanticDoc): Patch = {
    def isSealed(tpe: Type): Boolean = {
      tpe.symbol.info.exists(_.isSealed)
    }

    def leaksSealedParent(mods: List[Mod], templ: Template): Boolean =
      templ.inits.exists(i => isSealed(i.tpe)) &&
        mods.forall(m => !m.is[Mod.Final] && !m.is[Mod.Sealed])

    def addFinal(mods: Seq[Mod], d: Defn): Patch =
      (mods.find(!_.is[Mod.Annot]) match {
        case Some(mod) => Patch.addLeft(mod, "final ")
        case None =>
          mods.lastOption match {
            case Some(lastMod) => Patch.addRight(lastMod, " final")
            case None => Patch.addLeft(d, "final ")
          }
      }).atomic

    def isNonFinalConcreteCaseClass(mods: List[Mod]) =
      mods.exists(_.is[Mod.Case]) &&
        !mods.exists(m => m.is[Mod.Final] || m.is[Mod.Abstract])

    collect(sdoc.tree) {
      case (t @ Defn.Class(mods, _, _, _, _), parentPropagatesOuterRef)
          if isNonFinalConcreteCaseClass(mods) && !parentPropagatesOuterRef =>
        (Some(addFinal(mods, t)), PropagatesOuterRef)
      case (t @ Defn.Class(mods, _, _, _, templ), _)
          if leaksSealedParent(mods, templ) =>
        val lint = Patch.lint(
          Diagnostic("class", "Class extends sealed parent", t.pos)
        )
        (Some(lint), PropagatesOuterRef)
      case (t @ Defn.Trait(mods, _, _, _, templ), _)
          if leaksSealedParent(mods, templ) =>
        val lint = Patch.lint(
          Diagnostic("trait", "Trait extends sealed parent", t.pos)
        )
        (Some(lint), PropagatesOuterRef)
      case (_: Defn.Class | _: Defn.Trait, _) => (None, PropagatesOuterRef)
      case (_: Defn.Object | _: Template, outerRef) => (None, outerRef)
      case _ => (None, NoOuterRef)
    }
  }

  private val PropagatesOuterRef = true
  private val NoOuterRef = false

  private def collect(tree: Tree)(
      fn: (Tree, Boolean) => (Option[Patch], Boolean)): Patch = {
    var patch = Patch.empty
    var outerRef = NoOuterRef

    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        val prev = outerRef
        fn(tree, prev) match {
          case (Some(p), outer) =>
            patch += p
            outerRef = outer
          case (None, outer) =>
            outerRef = outer
        }
        super.apply(tree)
        outerRef = prev
      }
    }
    traverser(tree)
    patch
  }
}
