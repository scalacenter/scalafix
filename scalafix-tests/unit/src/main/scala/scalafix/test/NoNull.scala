package scalafix.test

import scala.meta._

import scalafix.v0.LintCategory
import scalafix.v1.Patch
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule

class NoNull extends SyntacticRule("NoNull") {
  val error: LintCategory = LintCategory.error("Nulls are not allowed.")

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect { case nil @ q"null" =>
      Patch.lint(error.at(nil.pos))
    }
  }.asPatch
}
