package scalafix.internal.rule

import scala.meta._
import scalafix.rule.SemanticRule
import scalafix.util.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory
import scalafix.util.SymbolMatcher
import scalafix.syntax._

case class Disable(index: SemanticdbIndex)
    extends SemanticRule(index, "Disable")
    with Product {

  private lazy val errorCategory: LintCategory = LintCategory.error(
    """Some constructs are unsafe to use and should be avoided""".stripMargin
  )

  private lazy val disabledSymbol: SymbolMatcher = 
    SymbolMatcher.exact(Disable.disabledSymbol)

  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.index.names.collect {
      case ResolvedName(pos, disabledSymbol(Symbol.Global(_, signature)), false) =>
        errorCategory
          .copy(id = signature.name)
          .at(s"${ signature.name } is disabled", pos)
    }
}

case object Disable {
  lazy val disabledSymbol: Symbol =
    Symbol("_root_.scala.Any#asInstanceOf()Ljava/lang/Object;.")
}
