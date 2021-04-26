package test

object ExplicitUnit {
  trait A {
    def x: Unit
  }
  abstract class B {
    def x: Unit
  }
  trait C {
    def x: Unit /* comment */
  }
  trait D {
    def x(): Unit
  }
  trait E {
    def x(a: String, b: Boolean): Unit
  }
  trait F {
    def x: String // don't touch this
  }
}
