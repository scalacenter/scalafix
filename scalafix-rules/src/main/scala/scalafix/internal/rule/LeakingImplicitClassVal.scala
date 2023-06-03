package scalafix.internal.rule

import scala.meta._

import scalafix.v1._

class LeakingImplicitClassVal extends SyntacticRule("LeakingImplicitClassVal") {

  override def description: String =
    "Adds 'private' to val parameters of implicit value classes"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case Defn.Class.After_4_6_0(
            cMods,
            _,
            _,
            Ctor.Primary.After_4_6_0(
              _,
              _,
              Term.ParamClause(Term.Param(pMods, _, _, _), _) :: Nil
            ),
            Template.After_4_4_0(
              _,
              Init.After_4_6_0(Type.Name("AnyVal"), _, _) :: Nil,
              _,
              _,
              _
            )
          ) if cMods.exists(_.is[Mod.Implicit]) =>
        val optPatch = for {
          anchorMod <- pMods.find(!_.is[Mod.Annot])
          if !pMods.exists(m => m.is[Mod.Private] || m.is[Mod.Protected])
        } yield Patch.addLeft(anchorMod, "private ")
        optPatch.asPatch
    }.asPatch
  }
}
