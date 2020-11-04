package scalafix.tests.v1

import scala.meta._

import org.scalatest.funsuite.AnyFunSuite
import scalafix.tests.core.BaseSemanticSuite
import scalafix.v1._

class SymbolMatcherSuite extends AnyFunSuite {
  implicit val doc: SemanticDocument =
    BaseSemanticSuite.loadDoc("SymbolMatcherTest.scala")

  test("exact") {
    val option = SymbolMatcher.exact("scala/Option#")
    assert(option.matches(Symbol("scala/Option#")))
    assert(!option.matches(Symbol("scala.Option#")))
    assert(!option.matches(Symbol("scala.Option.")))
    assert(!option.matches(Symbol("scala/Option.")))
    assert(!option.matches(Symbol("scala/Option().")))
    assert(!option.matches(Symbol("scala/Option")))
  }

  test("normalized") {
    List(
      "scala.Option",
      "scala/Option#",
      "scala.Option."
    ).foreach { sym =>
      val option = SymbolMatcher.normalized(sym)
      assert(option.matches(Symbol("scala/Option#")), sym)
      assert(option.matches(Symbol("scala/Option.")), sym)
      assert(option.matches(Symbol("scala/Option().")), sym)
      assert(!option.matches(Symbol("scala/Option")), sym)
    }

    List(
      "scala.Option.map().",
      "scala.Option#map().",
      "scala/Option#map().",
      "scala/Option#map(+1).",
      "scala.Option.map"
    ).foreach { sym =>
      val map = SymbolMatcher.normalized(sym)
      assert(map.matches(Symbol("scala/Option.map().")), sym)
      assert(map.matches(Symbol("scala/Option#map().")), sym)
      assert(map.matches(Symbol("scala/Option#map(+1).")), sym)
      assert(map.matches(Symbol("scala/Option.map(+1).")), sym)
    }
  }

  test("matches/unapply") {
    val symbolMatcher =
      SymbolMatcher.exact("test/SymbolMatcherTest.")
    val assertions = doc.tree.collect { case symbolMatcher(t @ Name(_)) =>
      assert(t.is[Term.Name])
      assert(t.parent.get.is[Defn.Object])
      assert(symbolMatcher.matches(t))
    }
    assert(assertions.length == 1)
  }
  test("+") {
    val mainClass = SymbolMatcher.exact("com/Main#")
    val mainObject = SymbolMatcher.exact("com/Main.")
    val main = mainClass + mainObject
    assert(main.matches(Symbol("com/Main#")))
    assert(main.matches(Symbol("com/Main.")))
  }

}
