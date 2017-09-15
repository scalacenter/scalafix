package scalafix.internal.rule

import scala.meta._
import scalafix.rule.SemanticRule
import scalafix.util.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory
import scalafix.syntax._

case class AsInstanceOfDisabled(index: SemanticdbIndex)
    extends SemanticRule(index, "AsInstanceOfDisabled")
    with Product {

  val errorCategory: LintCategory = LintCategory.error(
    """asInstanceOf is unsafe in isolation and violates parametricity when guarded by isInstanceOf.""".stripMargin
  )
 
  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.tree.collect {
      case ap@ Term.ApplyType(Term.Select(_, s: Term.Name), _) if s.symbol.contains(AsInstanceOfDisabled.disabledSymbol) => 
        errorCategory.at(s"$s is disabled", s.pos)
    }
}

case object AsInstanceOfDisabled {

  lazy val disabledSymbol: Symbol =
    Symbol("_root_.scala.Any#asInstanceOf()Ljava/lang/Object;.")
}
