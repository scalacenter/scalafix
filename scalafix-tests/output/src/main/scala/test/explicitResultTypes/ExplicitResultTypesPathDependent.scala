package test.explicitResultTypes

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: x.C = new x.C
    def gimme(yy: x.C) = ???; gimme(y)
  }
  implicit val b: _root_.test.explicitResultTypes.ExplicitResultTypesPathDependent.Path#B = new Path().x
}
