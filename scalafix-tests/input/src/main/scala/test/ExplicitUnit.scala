/*
rules = ProcedureSyntax
 */
package test

object ExplicitUnit {
  trait A {
    def x
  }
  abstract class B {
    def x
  }
  trait C {
    def x /* comment */
  }
  trait D {
    def x()
  }
  trait E {
    def x(a: String, b: Boolean)
  }
  trait F {
    def x: String // don't touch this
  }
}

