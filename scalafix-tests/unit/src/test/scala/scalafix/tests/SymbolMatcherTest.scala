package scalafix.tests

import scala.meta._
import scalafix.syntax._
import scalafix.util.SymbolMatcher

class SymbolMatcherTest extends BaseSemanticTest("SymbolMatcherTest") {

  test("matches/unapply") {
    val explicitReturn =
      SymbolMatcher.exact(Symbol("_root_.test.SymbolMatcherTest."))
    val source = attrs.input.parse[Source].get
    val assertions = source.collect {
      case explicitReturn(t @ Name(_)) if t.matches(explicitReturn) =>
        assert(t.is[Term.Name])
        assert(t.parent.get.is[Defn.Object])
    }
    assert(assertions.nonEmpty)
  }

}
