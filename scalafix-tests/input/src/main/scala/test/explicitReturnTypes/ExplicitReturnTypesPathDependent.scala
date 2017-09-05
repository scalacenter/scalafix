/*
rewrites = ExplicitReturnTypes
 */
package test.explicitReturnTypes

object ExplicitReturnTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x = new B
    implicit val y = new x.C
    def gimme(yy: x.C) = ???; gimme(y)
  }
  implicit val b = new Path().x
}
