/*
rules = NonUnitStatements
 */
package test

object NonUnitStatements {
  def f(a: Int): Int = 0

  val a = 42
  val b = 24

  object A {
    case class Foo() {
      def bar(a: Int): Int = 1
    }
    def foo: Foo = Foo()

    def unitFoo: Unit = {
      'symbol        // assert: NonUnitStatements._root_.scala.Symbol
      println("foo") // ok
    }
  }

  false           // assert: NonUnitStatements._root_.scala.Boolean
  !false          // assert: NonUnitStatements._root_.scala.Boolean
  ()              // ok, it's just Unit
  println("unit") // ok
  System.gc()     // ok
  f(a)            // assert: NonUnitStatements._root_.scala.Int
  a + b           // assert: NonUnitStatements._root_.scala.Int
  123             // assert: NonUnitStatements._root_.scala.Int
  A.foo.bar(b)    // assert: NonUnitStatements._root_.scala.Int
  A.foo           // assert: NonUnitStatements._root_.test.NonUnitStatements.A.Foo
  A.unitFoo       // ok

  a :: List(b)             // assert: NonUnitStatements._root_.scala.collection.immutable.List[B]
  List(1, 2, 3).map(_ * 2) // assert: NonUnitStatements.That
  // scala collections ¯\_(ツ)_/¯

  val c = {
    val a = 1
    val b = 2
    123 // assert: NonUnitStatements._root_.scala.Int
  }
}
