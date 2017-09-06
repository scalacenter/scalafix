package scalafix.cli

import scalafix.lint.LintCategory
import scalafix.rewrite.Rule

object TestRewrites {
  val LintError: Rule = Rule.syntactic { ctx =>
    val failure = LintCategory.error("Error!")
    ctx.lint(failure.at(ctx.tree.pos))
  }
  val LintWarning: Rule = Rule.syntactic { ctx =>
    val warning = LintCategory.warning("Warning!")
    ctx.lint(warning.at(ctx.tree.pos))
  }
}
