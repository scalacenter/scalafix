package scalafix.internal.rule

import scala.meta._
import metaconfig.{Conf, Configured}
import scalafix.rule.SemanticRule
import scalafix.util.SemanticdbIndex
import scalafix.rule.{Rule, RuleCtx}
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory
import scalafix.util.SymbolMatcher
import scalafix.internal.config.DisableConfig
import scalafix.internal.config.DisableConfigDecoder._
import scalafix.syntax._

final case class Disable(index: SemanticdbIndex, configuration: DisableConfig)
    extends SemanticRule(index, "Disable")
    with Product {

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Some constructs are unsafe to use and should be avoided""".stripMargin
    )

  private lazy val disabledSymbol: SymbolMatcher =
    SymbolMatcher.exact(
      Disable.disabledSymbol ::: configuration.disabledSymbols: _*)

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("Disable")(DisableConfig.empty)
      .map(Disable(index, _))

  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.index.names.collect {
      case ResolvedName(
          pos,
          disabledSymbol(Symbol.Global(_, signature)),
          false) =>
        errorCategory
          .copy(id = signature.name)
          .at(s"${signature.name} is disabled", pos)
    }
}

case object Disable {
  lazy val disabledSymbol: List[Symbol] =
    Symbol("_root_.scala.Any#asInstanceOf()Ljava/lang/Object;.") :: Nil
}
