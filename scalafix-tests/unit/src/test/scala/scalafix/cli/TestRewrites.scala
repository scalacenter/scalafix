package scalafix.cli

import scalafix.lint.LintCategory
import scalafix.rewrite.Rewrite

object TestRewrites {
  val LintError: Rewrite = Rewrite.syntactic { ctx =>
    val failure = LintCategory.error("Error!")
    ctx.lint(failure.at(ctx.tree.pos))
  }
  val LintWarning: Rewrite = Rewrite.syntactic { ctx =>
    val warning = LintCategory.warning("Warning!")
    ctx.lint(warning.at(ctx.tree.pos))
  }
}
