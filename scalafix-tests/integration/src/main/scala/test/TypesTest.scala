// scalafix:off
package test

class TypesTest {
  val a = 42
  private val b = List(42)
  class Inner
  val c = new TypesTest
  val d = new c.Inner
  private val e = null.asInstanceOf[TypesTest#Inner]
  val f: {
    def foo(a: Int): Int
    def bar(a: Int): Int
  } = ???
}
