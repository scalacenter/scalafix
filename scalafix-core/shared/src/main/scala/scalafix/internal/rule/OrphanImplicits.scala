package scalafix.internal.rule

import scala.meta._
import scalafix.internal.util.{SymbolOps, TreeOps}
import scalafix.rule.RuleCtx
import scalafix.syntax._
import scalafix.{LintCategory, LintMessage, SemanticRule, SemanticdbIndex}

final case class OrphanImplicits(index: SemanticdbIndex)
    extends SemanticRule(index, "OrphanImplicits") {

  override def description: String =
    "Linter that reports an error on implicit instances of shape F[G] " +
      "that do not belong to the companion object of F or G."

  private lazy val errorCategory: LintCategory =
    LintCategory.error("")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {

    def handleImplicit(
        tpe: Type,
        obj: Symbol.Global,
        pos: Position): Option[LintMessage] = {

      val tpeSymbols = tpe match {
        case Type.Apply(t, Seq(arg)) =>
          println(tpe)
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
                 |This definition is only allowed in one of the following objects:
                 |${tpeSymbols.mkString(", ")}
                """.stripMargin,
              pos
            ))
      } else {
        None
      }
    }
    def handleDefn(
        mods: List[Mod],
        tpe: Option[Type],
        d: Defn,
        obj: Symbol.Global): Option[LintMessage] = {
      if (mods.exists(_.is[Mod.Implicit])) {
        tpe match {
          case Some(t) => handleImplicit(t, obj, d.pos)
          case None =>
            for {
              name <- TreeOps.defnName(d)
              symbol <- name.symbol
              tpe <- symbol.resultType
              res <- handleImplicit(tpe, obj, d.pos)
            } yield res
        }
      } else {
        None
      }
    }

    ctx.tree.collect {
      case Defn.Object(_, name @ index.Symbol(sym), templ) =>
        sym match {
          case obj: Symbol.Global =>
            templ.stats.collect {
              case t @ Defn.Val(mods, _, tpe, _) =>
                handleDefn(mods, tpe, t, obj)
              case t @ Defn.Var(mods, _, tpe, _) =>
                handleDefn(mods, tpe, t, obj)
              case t @ Defn.Def(mods, _, _, _, tpe, _) =>
                handleDefn(mods, tpe, t, obj)
            }.flatten
          case _ => List.empty
        }
    }.flatten
  }
}
