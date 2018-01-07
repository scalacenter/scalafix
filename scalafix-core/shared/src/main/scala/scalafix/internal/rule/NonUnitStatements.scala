package scalafix.internal.rule

import scala.meta.Term.Block
import scala.meta._
import scalafix.SemanticdbIndex
import scalafix.internal.util.TypeSyntax
import scalafix.lint.{LintCategory, LintMessage}
import scalafix.rule.{RuleCtx, RuleName, SemanticRule}
import scalafix.syntax._
import scala.meta.contrib.implicits.Equality._

case class NonUnitStatements(index: SemanticdbIndex)
    extends SemanticRule(index, RuleName("NonUnitStatements")) {

  override def description: String =
    "This rule ensures that statements should only return Unit"

  private lazy val errorCategory: LintCategory =
    LintCategory.error("Statements should only return Unit")

  def litType(l: Lit): Option[Type] = l match {
    case Lit.Int(_) => Some(t"_root_.scala.Int")
    case Lit.Unit() => Some(t"_root_.scala.Unit")
    case Lit.Float(_) => Some(t"_root_.scala.Float")
    case Lit.Double(_) => Some(t"_root_.scala.Double")
    case Lit.Symbol(_) => Some(t"_root_.scala.Symbol")
    case Lit.Boolean(_) => Some(t"_root_.scala.Boolean")
    case Lit.Byte(_) => Some(t"_root_.scala.Byte")
    case Lit.Char(_) => Some(t"_root_.scala.Char")
    case Lit.Long(_) => Some(t"_root_.scala.Long")
    case Lit.Null() => Some(t"_root_.scala.Null")
    case Lit.String(_) => Some(t"_root_.scala.String")
    case Lit.Short(_) => Some(t"_root_.scala.Short")
    case _ => None
  }

  def statementType(stat: Stat): Option[Type] = stat match {
    case l: Lit => litType(l)
    case name: Term.Name => name.symbol.flatMap(_.resultType)
    case Term.Select(_, name) => statementType(name)
    case Term.Apply(fun, _) => statementType(fun)
    case Term.ApplyInfix(_, op, _, _) => statementType(op)
    case Term.ApplyUnary(op, _) => statementType(op)
    case _ => None
  }

  override def check(ctx: RuleCtx): List[LintMessage] = {
    def statsToErrors(stats: List[Stat]): List[LintMessage] =
      for {
        stat <- stats
        statType <- statementType(stat)
        (fullType, _) = TypeSyntax.prettify(statType, ctx, false)
        if !fullType.isEqual(t"_root_.scala.Unit")
      } yield {
        errorCategory
          .copy(id = fullType.toString)
          .at(s"Type $fullType is not Unit", stat.pos)
      }

    def actualStats(defName: Term.Name, stats: List[Stat]): List[Stat] = {
      val fullTypeOpt = defName.symbol
        .flatMap(t => t.resultType)
        .map(TypeSyntax.prettify(_, ctx, false))
      fullTypeOpt match {
        case Some((tpe, _)) if tpe.isEqual(t"_root_.scala.Unit") => stats
        case _ => stats.dropRight(1)
      }
    }

    ctx.tree.collect {
      case Template(_, _, _, stats) => statsToErrors(stats)
      case Defn.Val(_, Pat.Var(name) :: Nil, _, Block(stats)) =>
        statsToErrors(actualStats(name, stats))
      case Defn.Var(_, Pat.Var(name) :: Nil, _, Some(Block(stats))) =>
        statsToErrors(actualStats(name, stats))
      case Defn.Def(_, name, _, _, _, Block(stats)) =>
        statsToErrors(actualStats(name, stats))
    }.flatten
  }
}
