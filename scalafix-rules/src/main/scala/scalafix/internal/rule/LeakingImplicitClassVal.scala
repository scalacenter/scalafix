package scalafix.internal.rule

import scala.meta._

import scalafix.util.TreeOps
import scalafix.v1._

class LeakingImplicitClassVal extends SyntacticRule("LeakingImplicitClassVal") {

  override def description: String =
    "Adds 'private' to val parameters of implicit value classes"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    TreeOps.collectTree {
      case Defn.Class.Initial(
            cMods,
            _,
            _,
            Ctor.Primary.Initial(
              _,
              _,
              (Term.Param(pMods, _, _, _) :: Nil) :: Nil
            ),
            Template.Initial(
              _,
              Init.Initial(Type.Name("AnyVal"), _, _) :: Nil,
              _,
              _
            )
          ) if cMods.exists(_.is[Mod.Implicit]) =>
        val optPatch =
          if (pMods.exists(m => m.is[Mod.Private] || m.is[Mod.Protected])) None
          else pMods.find(!_.is[Mod.Annot]).map(Patch.addLeft(_, "private "))
        optPatch.asPatch
    }(doc.tree).asPatch
  }
}
