/*
rules = DisableSyntax
DisableSyntax.keywords = [
  var
  null
  return
  throw
]
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
DisableSyntax.noCovariantTypes = true
DisableSyntax.noContravariantTypes = true
DisableSyntax.noDefaultArgs = true
DisableSyntax.noValInAbstract = true
DisableSyntax.noImplicitObject = true
DisableSyntax.noImplicitConversion = true
DisableSyntax.regex = [
  {
    id = offensive
    pattern = "[P|p]imp"
    message = "Please consider a less offensive word such as Extension"
  }
  "Await\\.result"
]
*/
package test

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case object DisableSyntax {


  null /* assert: DisableSyntax.keywords.null
  ^
  null is disabled
  */

  var a = 1                 // assert: DisableSyntax.keywords.var
  def foo: Unit = return    // assert: DisableSyntax.keywords.return
  throw new Exception("ok") // assert: DisableSyntax.keywords.throw

  "semicolon";              // assert: DisableSyntax.noSemicolons
  <a>xml</a>                // assert: DisableSyntax.noXml
	                          // assert: DisableSyntax.noTabs

  implicit class StringPimp(value: String) { // assert: DisableSyntax.offensive
    def -(other: String): String = s"$value - $other"
  }

  // actually 7.5 million years
  Await.result(Future(42), 75.days) // assert: DisableSyntax.Await\.result

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
