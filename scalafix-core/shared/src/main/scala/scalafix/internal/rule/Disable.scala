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

  private lazy val disabledSymbol: SymbolMatcher =
    SymbolMatcher.normalized(config.allSymbols: _*)

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disable", "Disable")(DisableConfig.default)
      .map(Disable(index, _))

  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.index.names.collect {
      case ResolvedName(
          pos,
          disabledSymbol(symbol @ Symbol.Global(_, signature)),
          false) => {

        val message =
          config
            .customMessage(symbol)
            .getOrElse(s"${signature.name} is disabled")

        errorCategory
          .copy(id = signature.name)
          .at(message, pos)
      }
    }
}
