package scalafix.tests.cli

import scalafix.v0.LintCategory
import scalafix.v1._

class LintError extends SyntacticRule("LintError") {
  override def fix(implicit doc: Doc): Patch = {
    val failure = LintCategory.error("failure", "Error!")
    Patch.lint(failure.at(doc.tree.pos))
  }
}

class LintWarning extends SyntacticRule("LintWarning") {
  override def fix(implicit doc: Doc): Patch = {
    val failure = LintCategory.warning("warning", "Warning!")
    Patch.lint(failure.at(doc.tree.pos))
  }
}
