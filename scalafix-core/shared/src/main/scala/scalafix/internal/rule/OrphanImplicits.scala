package scalafix.internal.rule

import scala.meta._
import scalafix.internal.util.SymbolOps
import scalafix.lint.LintCategory
import scalafix.rule.RuleCtx
import scalafix.util.SemanticdbIndex
import scalafix.{LintMessage, Rule}

final case class OrphanImplicits(index: SemanticdbIndex)
    extends Rule("OrphanImplicits") {

  override def description: String =
    "Linter that reports an error on implicit instances of shape F[G] " +
      "that do not belong in the companion objects of F or G."

  private lazy val errorCategory: LintCategory =
    LintCategory.error("Orphan implicits should be avoided")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {

    def handleImplicit(
        tpe: Type,
        obj: Symbol.Global,
        pos: Position): Option[LintMessage] = {

      val tpeSymbols = tpe match {
        case Type.Apply(t, Seq(arg)) =>
          Seq(index.symbol(t), index.symbol(arg)).flatten
        case _ => Seq.empty
      }

      if (tpeSymbols.nonEmpty && !tpeSymbols.exists(
          SymbolOps.isSameNormalized(obj, _)
        )) {
        Some(
          errorCategory
            .at(
              s"""Orphan implicits are not allowed.
                 |You should put this definition to one of the following objects:
                 |${tpeSymbols.mkString(", ")}
                """.stripMargin,
              pos
            ))
      } else {
        None
      }
    }

    ctx.tree.collect {
      case Defn.Object(_, name, templ) =>
        index.symbol(name) match {
          case Some(obj @ Symbol.Global(_, _)) =>
            templ.stats.collect {
              case t @ Defn.Val(mods, _, Some(tpe), _)
                  if mods.exists(_.is[Mod.Implicit]) =>
                handleImplicit(tpe, obj, t.pos)
              case t @ Defn.Def(mods, _, _, _, Some(tpe), _)
                  if mods.exists(_.is[Mod.Implicit]) =>
                handleImplicit(tpe, obj, t.pos)
            }.flatten
          case None => List.empty
        }
    }.flatten
  }
}
