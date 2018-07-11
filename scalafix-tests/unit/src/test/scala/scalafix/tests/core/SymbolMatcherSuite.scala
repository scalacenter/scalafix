package scalafix.tests.core

import scala.meta._
import scalafix.v0._
import scalafix.syntax._
import scalafix.util.SymbolMatcher

class SymbolMatcherSuite extends BaseSemanticSuite("SymbolMatcherTest") {

  test("matches/unapply") {
    val symbolMatcher =
      SymbolMatcher.exact(Symbol("test/SymbolMatcherTest."))
    val assertions = source.collect {
      case symbolMatcher(t @ Name(_)) =>
        assert(t.is[Term.Name])
        assert(t.parent.get.is[Defn.Object])
        assert(symbolMatcher.matches(t))
        assert(t.matches(symbolMatcher))
    }
    assert(assertions.length == 1)
  }

  test("normalized") {
    val term = SymbolMatcher.normalized(Symbol("Foo.a."))
    assert(term.matches(Symbol("Foo.a#"))) // type
    assert(term.matches(Symbol("Foo#a()."))) // method
    assert(!term.matches(Symbol("Foo.a.apply()."))) // apply
  }

}
