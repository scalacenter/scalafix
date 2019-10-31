
package test.explicitResultTypes

object ExplicitResultTypesPathDependent {
  class Path {
    class B { class C }
    implicit val x: B = new B
    implicit val y: x.C = new x.C
    def gimme(yy: x.C): Nothing = identity(???); gimme(y)
  }
  implicit val b: Path#B = new Path().x
  trait Foo[T] {
    type Self
    def bar: Self
  }
  implicit def foo[T]: Foo[T]#Self = null.asInstanceOf[Foo[T]].bar
}
