package scalafix.internal.rule

import scala.meta._
import scalafix.internal.util.{ContextTraverser, SymbolOps}
import scalafix.lint.LintCategory
import scalafix.rule.RuleCtx
import scalafix.util.SemanticdbIndex
import scalafix.{LintMessage, Rule}

final case class OrphanImplicits(index: SemanticdbIndex)
    extends Rule("OrphanImplicits") {

  override def description: String =
    "Linter bans definitions of implicits F[G] unless they are on the companion of the F or the G"

  private lazy val errorCategory: LintCategory =
    LintCategory.error("Orphan implicits should be avoided")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    def symbolsFromType(tpe: Type): List[Symbol.Global] = tpe match {
      case t: Type.Name =>
        index.symbol(t) match {
          case Some(s: Symbol.Global) => List(s)
          case _ => List.empty
        }
      case Type.Apply(tpe, args) =>
        symbolsFromType(tpe) ::: args.flatMap(symbolsFromType)
      // only Apply and Name for now
      case _ => List.empty
    }

    def handleImplicit(
        tpe: Type,
        objs: List[Symbol.Global],
        pos: Position): Either[LintMessage, List[Symbol.Global]] = {
      val symbols = symbolsFromType(tpe)
      if (symbols.exists(
          s => objs.exists(o => SymbolOps.isSameNormalized(o, s)))) {
        Right(objs)
      } else {
        Left(
          errorCategory
            .at(
              s"""Orphan implicits are not allowed.
                 |You should put this definition to one of the following objects:
                 |${symbols.mkString(", ")}
                """.stripMargin,
              pos
            ))
      }
    }
    new ContextTraverser[LintMessage, List[Symbol.Global]](List.empty)({
      case (Defn.Object(_, name, _), objs) =>
        index.symbol(name) match {
          case Some(s @ Symbol.Global(_, _)) => Right(s :: objs)
          case None => Right(objs)
        }
      case (t @ Defn.Val(mods, _, Some(tpe), _), objs)
          if mods.exists(_.is[Mod.Implicit]) =>
        handleImplicit(tpe, objs, t.pos)
      case (t @ Defn.Def(mods, _, _, _, Some(tpe), _), objs)
          if mods.exists(_.is[Mod.Implicit]) =>
        handleImplicit(tpe, objs, t.pos)
    }).result(ctx.tree)
  }
}
