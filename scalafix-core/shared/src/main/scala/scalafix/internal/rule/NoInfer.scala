package scalafix.internal.rule

import scalafix.v0._
import metaconfig.{Conf, Configured}
import scalafix.lint.LintCategory
import scalafix.lint.LintMessage
import scalafix.rule.{Rule, RuleCtx}
import scalafix.rule.SemanticRule
import scalafix.util.SemanticdbIndex
import scalafix.util.SymbolMatcher
import scalafix.internal.config.NoInferConfig

final case class NoInfer(index: SemanticdbIndex, config: NoInferConfig)
    extends SemanticRule(index, "NoInfer")
    with Product {

  private lazy val error: LintCategory =
    LintCategory.error(
      """The Scala compiler sometimes infers a too generic type such as Any.
        |If this is intended behavior, then the type should be explicitly type
        |annotated in the source.""".stripMargin
    )

  override def description: String =
    "Linter for types that the Scala compiler cannot infer."

  private lazy val noInferSymbol: SymbolMatcher =
    if (config.symbols.isEmpty)
      SymbolMatcher.normalized(NoInferConfig.badSymbols: _*)
    else SymbolMatcher.normalized(config.symbols: _*)

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("noInfer", "NoInfer")(NoInferConfig.default)
      .map(NoInfer(index, _))

  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.index.synthetics.flatMap {
      case Synthetic(pos, _, names) =>
        names.collect {
          case ResolvedName(_, noInferSymbol(Symbol.Global(_, signature)), _) =>
            val categoryId = signature.name.toLowerCase()
            error
              .copy(id = categoryId)
              .at(s"Inferred ${signature.name}", pos)
        }
    }
}

case object NoInfer {

  def badSymbolNames: List[String] = NoInferConfig.badSymbols.collect {
    case Symbol.Global(_, signature) => signature.name
  }
}
