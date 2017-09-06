package scalafix.cli

import scalafix.lint.LintCategory
import scalafix.rule.Rule

object TestRules {
  val LintError: Rule = Rule.syntactic("LintError") { ctx =>
    val failure = LintCategory.error("failure", "Error!")
    ctx.lint(failure.at(ctx.tree.pos))
  }
  val LintWarning: Rule = Rule.syntactic("LintWarning") { ctx =>
    val warning = LintCategory.warning("warning", "Warning!")
    ctx.lint(warning.at(ctx.tree.pos))
  }
}
