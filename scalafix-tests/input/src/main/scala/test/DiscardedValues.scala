/*
rules = DiscardedValues
 */
package test

object DiscardedValues {
  def f(a: Int): Int = 0

  val a = 42
  val b = 24

  object A {
    case class Foo() {
      def bar(a: Int): Int = 1
    }
    def foo: Foo = Foo()

    def unitFoo: Unit = {
      'symbol        // assert: DiscardedValues._root_.scala.Symbol
      println("foo") // ok
    }
  }

  false           // assert: DiscardedValues._root_.scala.Boolean
  !false          // assert: DiscardedValues._root_.scala.Boolean
  ()              // ok, it's just Unit
  println("unit") // ok
  System.gc()     // ok
  f(a)            // assert: DiscardedValues._root_.scala.Int
  a + b           // assert: DiscardedValues._root_.scala.Int
  123             // assert: DiscardedValues._root_.scala.Int
  A.foo.bar(b)    // assert: DiscardedValues._root_.scala.Int
  A.foo           // assert: DiscardedValues._root_.test.DiscardedValues.A.Foo
  A.unitFoo       // ok

  a :: List(b)             // assert: DiscardedValues._root_.scala.collection.immutable.List[B]
  List(1, 2, 3).map(_ * 2) // assert: DiscardedValues.That
  // scala collections ¯\_(ツ)_/¯

  def f = {
    val a = 1
    val b = 3
    a + b
  }

  def fUnit: Unit = {
    val a = 1
    val b = 3
    a + b // assert: DiscardedValues._root_.scala.Int
  }

  var g: Int = {
    f // assert: DiscardedValues._root_.scala.Int
    fUnit
    123
  }

  val h = {
    val c = 123
    val d = {
      val ff = f
      ff + c
    }
    d
  }
}
