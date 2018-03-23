package test.explicitResultTypes

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: Path.this.x.C = new x.C
    def gimme(yy: x.C) = ???; gimme(y)
  }
  implicit val b: _root_.test.explicitResultTypes.ExplicitResultTypesPathDependent.Path#B = new Path().x
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: _root_.test.explicitResultTypes.ExplicitResultTypesPathDependent.Foo[T]#Self = null.asInstanceOf[Foo[T]].bar
}
