/*
rules = DisableSyntax
DisableSyntax.noCovariantTypes = true
DisableSyntax.noContravariantTypes = true
DisableSyntax.noDefaultArgs = true
DisableSyntax.noValInAbstract = true
DisableSyntax.noImplicitObject = true
DisableSyntax.noImplicitConversion = true
*/
package test

object DisableSyntaxMoreRules {
  class Co[+T](t: T) // assert: DisableSyntax.covariant
  class Contra[-T](t: T) // assert: DisableSyntax.contravariant

  class Pro[-A,             // assert: DisableSyntax.contravariant
  +B](a: A, b: B) // assert: DisableSyntax.covariant

  trait TraitCo[+T]            // assert: DisableSyntax.covariant
  type TypeCo[+T] = TraitCo[T] // assert: DisableSyntax.covariant

  class Foo {
    def bar(x: Int = 42) = 1 /* assert: DisableSyntax.defaultArgs
                     ^
Default args makes it hard to use methods as functions.
*/
  }

  trait FooT {
    def bar(x: Int = 42) = ???           // assert: DisableSyntax.defaultArgs
    def foo(a: Int)(b: Int = 1) = ???    // assert: DisableSyntax.defaultArgs
    def foobar(a: Int, b: Int = 1) = ??? // assert: DisableSyntax.defaultArgs

    def muli(
      a: Int = 1, // assert: DisableSyntax.defaultArgs
      b: Int = 2  // assert: DisableSyntax.defaultArgs
    )(
      c: Int = 3, // assert: DisableSyntax.defaultArgs
      d: Int = 4  // assert: DisableSyntax.defaultArgs
    ) = ???
  }

  trait FooTT {
    def bar(x: Int) = 3 // ok
  }

  val fooT = new FooTT {
    override def bar(x: Int = 42) = 4 // assert: DisableSyntax.defaultArgs
  }

  def bar(x: Int = 42) = 5 // assert: DisableSyntax.defaultArgs
  def foobar = {
    def foobarfoo(x: Int = 42) = 6 // assert: DisableSyntax.defaultArgs
  }

  class Bar { type Foo = Int; def foo = 42 }
  def foo(a: { type Foo = Int; def foo: Foo } = new Bar): Int = a.foo /* assert: DisableSyntax.defaultArgs
                                                ^
Default args makes it hard to use methods as functions.
*/
  def foo2(a: { type Foo = Int; def foo: Foo } = {val a = new Bar; a}): Int = a.foo /* assert: DisableSyntax.defaultArgs
                                                 ^
Default args makes it hard to use methods as functions.
*/

  class Buzz(val a: Int = 1) // ok

  abstract class AbstractThing {
    val noooo = "yes" // assert: DisableSyntax.valInAbstract
  }

  trait AbstractInterface {
    val superImportantConst = 42 * 42 * 42 // assert: DisableSyntax.valInAbstract
  }

  class RealThing extends AbstractThing {
    val itsok = "indeed" // ok
  }

  val anon = new AbstractThing {
    val itsalsook = "indeed" // ok
  }

  implicit object FooImplicit extends Foo {} // assert: DisableSyntax.implicitObject

  implicit def toString(a: Any): String = a.toString // assert: DisableSyntax.implicitConversion
  implicit def toImplicitString(implicit foo: Foo) = foo.toString // ok
}
