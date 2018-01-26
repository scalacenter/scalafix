package scalafix.internal.rule

import scala.meta._
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.lint.LintMessage
import scalafix.rule.RuleName

case object NoFinalize
    extends Rule(
      RuleName.deprecated(
        "NoFinalize",
        "Use DisableSyntax.noFinalize instead",
        "0.5.8")) {

  override def description: String =
    "Deprecated, use DisableSyntax.noFinalize instead."

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect(DisableSyntax.FinalizeMatcher("")).flatten
  }
}
