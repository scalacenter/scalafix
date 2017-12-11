package scalafix.internal.rule

import metaconfig.{Conf, Configured}
import scala.meta._
import scalafix.internal.config.DisableConfig
import scalafix.lint.LintCategory
import scalafix.lint.LintMessage
import scalafix.rule.SemanticRule
import scalafix.rule.{Rule, RuleCtx}
import scalafix.syntax._
import scalafix.util.SemanticdbIndex
import scalafix.util.SymbolMatcher

final case class Disable(index: SemanticdbIndex, config: DisableConfig)
    extends SemanticRule(index, "Disable") {

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Some constructs are unsafe to use and should be avoided""".stripMargin
    )

  override def description: String =
    "Linter that reports an error on a configurable set of symbols."

  private lazy val disabledSymbol: SymbolMatcher =
    SymbolMatcher.normalized(config.allSymbols: _*)

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disable", "Disable")(DisableConfig.default)
      .map(Disable(index, _))

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    for {
      document <- ctx.index.documents.view
      ResolvedName(
        pos,
        disabledSymbol(symbol @ Symbol.Global(_, signature)),
        false
      ) <- {
        document.names.view ++
          document.synthetics.view.flatMap(_.names)
      }
    } yield {
      val (details, caret) = pos.input match {
        case synthetic @ Input.Synthetic(_, input, start, end) =>
          // For synthetics the caret should point to the original position
          // but display the inferred code.
          s" and it got inferred as `${synthetic.text}`" ->
            Position.Range(input, start, end)
        case _ =>
          "" -> pos
      }
      val message = config
        .customMessage(symbol)
        .getOrElse(s"${signature.name} is disabled$details")
      errorCategory
        .copy(id = signature.name)
        .at(message, caret)
    }
  }
}
