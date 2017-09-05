package test.explicitReturnTypes

object ExplicitReturnTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: _root_.test.explicitReturnTypes.ExplicitReturnTypesPathDependent.Path#B = new B
    implicit val y: x.C = new x.C
    def gimme(yy: x.C) = ???; gimme(y)
  }
  implicit val b: _root_.test.explicitReturnTypes.ExplicitReturnTypesPathDependent.Path#B = new Path().x
}
