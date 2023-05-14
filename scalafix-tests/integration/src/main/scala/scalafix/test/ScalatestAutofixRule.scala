package scalafix.test

import scalafix.v1.SemanticRule
import scalafix.v1._

class ScalatestAutofixRule extends SemanticRule("ScalatestAutofixRule") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    Patch.replaceSymbols(
      "org.scalatest_autofix.Matchers" -> "org.scalatest_autofix.matchers.should.Matchers"
    )
  }
}
