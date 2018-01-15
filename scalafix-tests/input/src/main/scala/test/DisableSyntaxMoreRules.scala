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
    def bar(x: Int = 42) = 1 // assert: DisableSyntax.defaultArgs
  }

  trait FooT {
    def bar(x: Int = 42) = ???           // assert: DisableSyntax.defaultArgs
    def foo(a: Int)(b: Int = 1) = ???    // assert: DisableSyntax.defaultArgs
    def foobar(a: Int, b: Int = 1) = ??? // assert: DisableSyntax.defaultArgs
  }

  trait FooTT {
    def bar(x: Int) = 3 // ok
  }

  val fooT = new FooTT {
    override def bar(x: Int = 42) = 4 // assert: DisableSyntax.defaultArgs
  }

  def bar(x: Int = 42) = 5 // ok
  def foobar = {
    def foobarfoo(x: Int = 42) = 6 // ok
  }

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
