package scalafix.test

import scala.meta._

import scalafix.v0.LintCategory
import scalafix.v1._

class NoDummy extends SyntacticRule("NoDummy") {
  val error: LintCategory = LintCategory.error("Dummy!")
  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case tree @ Name(name) if name.toLowerCase.contains("dummy") =>
        Patch.lint(error.at(tree.pos))
    }
  }.asPatch
}
