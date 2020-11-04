package scalafix.internal.rule

import scala.meta._

import scalafix.v1._

class LeakingImplicitClassVal extends SyntacticRule("LeakingImplicitClassVal") {

  override def description: String =
    "Adds 'private' to val parameters of implicit value classes"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case Defn.Class(
            cMods,
            _,
            _,
            Ctor.Primary(_, _, (Term.Param(pMods, _, _, _) :: Nil) :: Nil),
            Template(_, init"AnyVal" :: Nil, _, _)
          ) if cMods.exists(_.is[Mod.Implicit]) =>
        val optPatch = for {
          anchorMod <- pMods.find(!_.is[Mod.Annot])
          if !pMods.exists(m => m.is[Mod.Private] || m.is[Mod.Protected])
        } yield Patch.addLeft(anchorMod, "private ")
        optPatch.asPatch
    }.asPatch
  }
}
