package scalafix.tests.cli

import scalafix.Patch
import scalafix.lint.LintCategory
import scalafix.v1.Doc
import scalafix.v1.SyntacticRule


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
