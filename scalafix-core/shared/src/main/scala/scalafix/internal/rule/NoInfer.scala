package scalafix.internal.rule

import scala.meta._
import scalafix.lint.LintCategory
import scalafix.lint.LintMessage
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule
import scalafix.util.SemanticdbIndex
import scalafix.util.SymbolMatcher

case class NoInfer(index: SemanticdbIndex)
    extends SemanticRule(index, "NoInfer")
    with Product {
  val badSymbol: SymbolMatcher = SymbolMatcher.exact(
    Symbol("_root_.java.io.Serializable#"),
    Symbol("_root_.scala.Any#"),
    Symbol("_root_.scala.AnyVal#"),
    Symbol("_root_.scala.AnyVal#"),
    Symbol("_root_.scala.Product#")
  )
  val error: LintCategory =
    LintCategory.error(
      "",
      """The Scala compiler sometimes infers a too generic type such as Any.
        |If this is intended behavior, then the type should be explicitly type
        |annotated in the source.""".stripMargin
    )
  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.index.synthetics.flatMap {
      case Synthetic(pos, _, names) =>
        names.collect {
          case ResolvedName(_, badSymbol(Symbol.Global(_, signature)), _) =>
            val categoryId = signature.name.toLowerCase()
            error
              .copy(id = categoryId)
              .at(s"Inferred ${signature.name}", pos)
        }
    }
}
