package scalafix.tests.core

import scala.meta._
import scalafix.v0._
import scalafix.syntax._
import scalafix.util.SymbolMatcher

class SemanticdbIndexSuite extends BaseSemanticSuite("SemanticdbIndexTest") {

  test("symbol(Importee.Name)") {
    val mutable =
      SymbolMatcher.exact(Symbol("scala/collection/mutable/"))
    var hasAssert = false
    source.collect {
      case importee @ Importee.Name(name @ Name("mutable")) =>
        assert(index.symbol(importee) == index.symbol(name))
        assert(importee.matches(mutable))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Type.Select)") {
    val listBuffer =
      SymbolMatcher.exact(Symbol("scala/collection/mutable/ListBuffer#"))
    var hasAssert = false
    source.collect {
      case select @ Type.Select(_, name @ Type.Name("ListBuffer")) =>
        assert(index.symbol(select) == index.symbol(name))
        assert(select.matches(listBuffer))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Term.Select)") {
    val listBuffer =
      SymbolMatcher.exact(Symbol("scala/collection/mutable/ListBuffer."))
    var hasAssert = false
    source.collect {
      case select @ Term.Select(_, name @ Term.Name("ListBuffer")) =>
        assert(index.symbol(select) == index.symbol(name))
        assert(select.matches(listBuffer))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Importee.Rename)") {
    val success = SymbolMatcher.normalized(Symbol("scala/util/Success#"))
    var hasAssert = false
    source.collect {
      case importee @ Importee.Rename(name @ Name("Success"), _) =>
        assert(index.symbol(importee) == index.symbol(name))
        assert(success.matches(importee))
        hasAssert = true
    }
    assert(hasAssert)
  }

}
