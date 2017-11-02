package scalafix.tests.core

import scala.meta.ParseException
import scalafix.internal.rule.ProcedureSyntax
import scalafix.testkit.utest.SyntacticRuleSuite
import org.langmeta.inputs.Input

object ErrorSuite extends SyntacticRuleSuite {
  test("on parse error") {
    intercept[ParseException] {
      ProcedureSyntax.apply(Input.String("object A {"))
    }
  }
}
