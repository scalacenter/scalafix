package scalafix.cli

import scalafix.lint.LintID
import scalafix.rewrite.Rewrite

object TestRewrites {
  val LintError: Rewrite = Rewrite.syntactic { ctx =>
    val failure = LintID.error("Error!")
    ctx.lint(failure.at(ctx.tree.pos))
  }
  val LintWarning: Rewrite = Rewrite.syntactic { ctx =>
    val warning = LintID.warning("Warning!")
    ctx.lint(warning.at(ctx.tree.pos))
  }
}
