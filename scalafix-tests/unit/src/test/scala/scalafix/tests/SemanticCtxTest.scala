package scalafix.tests

import scala.meta._
import scalafix.util.SymbolMatcher
import scalafix.syntax._

class SemanticCtxTest extends BaseSemanticTest("SemanticCtxTest") {

  test("symbol(Importee.Name)") {
    val mutable =
      SymbolMatcher.exact(Symbol("_root_.scala.collection.mutable."))
    var hasAssert = false
    source.collect {
      case importee @ Importee.Name(name @ Name("mutable")) =>
        assert(sctx.symbol(importee) == sctx.symbol(name))
        assert(importee.matches(mutable))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Type.Select)") {
    val listBuffer =
      SymbolMatcher.exact(Symbol("_root_.scala.collection.mutable.ListBuffer#"))
    var hasAssert = false
    source.collect {
      case select @ Type.Select(_, name @ Type.Name("ListBuffer")) =>
        assert(sctx.symbol(select) == sctx.symbol(name))
        assert(select.matches(listBuffer))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Term.Select)") {
    val listBuffer =
      SymbolMatcher.exact(Symbol("_root_.scala.collection.mutable.ListBuffer."))
    var hasAssert = false
    source.collect {
      case select @ Term.Select(_, name @ Term.Name("ListBuffer")) =>
        assert(sctx.symbol(select) == sctx.symbol(name))
        assert(select.matches(listBuffer))
        hasAssert = true
    }
    assert(hasAssert)
  }

  test("symbol(Importee.Rename)") {
    val success =
      SymbolMatcher.exact(
        Symbol("_root_.scala.util.Success.;_root_.scala.util.Success#"))
    var hasAssert = false
    source.collect {
      case importee @ Importee.Rename(name @ Name("Success"), _) =>
        assert(sctx.symbol(importee) == sctx.symbol(name))
        assert(importee.matches(success))
        hasAssert = true
    }
    assert(hasAssert)
  }
}
