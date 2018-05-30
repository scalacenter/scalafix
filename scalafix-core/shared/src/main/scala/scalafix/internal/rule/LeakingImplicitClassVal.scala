package scalafix.internal.rule

import scala.meta._
import scalafix.v0._

case object LeakingImplicitClassVal extends Rule("LeakingImplicitClassVal") {

  override def description: String =
    "Add private access modifier to val parameters of implicit value classes in order to prevent public access"

  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case Defn.Class(
          cMods,
          _,
          _,
          Ctor.Primary(_, _, (Term.Param(pMods, _, _, _) :: Nil) :: Nil),
          Template(_, init"AnyVal" :: Nil, _, _))
          if cMods.exists(_.is[Mod.Implicit]) =>
        val optPatch = for {
          anchorMod <- pMods.find(!_.is[Mod.Annot])
          if !pMods.exists(m => m.is[Mod.Private] || m.is[Mod.Protected])
        } yield ctx.addLeft(anchorMod, "private ")
        optPatch.asPatch

    }.asPatch
  }
}
