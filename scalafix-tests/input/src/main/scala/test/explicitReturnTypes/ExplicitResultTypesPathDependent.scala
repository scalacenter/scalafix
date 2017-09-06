/*
rules = ExplicitResultTypes
 */
package test.explicitReturnTypes

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x = new B
    implicit val y = new x.C
    def gimme(yy: x.C) = ???; gimme(y)
  }
  implicit val b = new Path().x
}
