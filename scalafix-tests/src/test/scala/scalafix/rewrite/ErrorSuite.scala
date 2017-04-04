package scalafix.rewrite

import scalafix.Failure
import scalafix.Fixed
import scalafix.Scalafix

class ErrorSuite extends RewriteSuite(ProcedureSyntax) {

  test("on parse error") {
    val Fixed.Failed(err: Failure.ParseError) = Scalafix.fix("object A {")
  }
}
