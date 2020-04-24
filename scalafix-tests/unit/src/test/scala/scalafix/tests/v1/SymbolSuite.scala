package scalafix.tests.v1

import scala.meta._
import scalafix.tests.core.BaseSemanticSuite
import scalafix.v1._

class SymbolSuite extends munit.FunSuite {
  implicit val doc = BaseSemanticSuite.loadDoc("SymbolTest.scala")

  test("normalized") {
    val ref :: Nil = doc.tree.collect {
      case Import(Importer(ref, _) :: Nil) => ref
    }

    assertEquals(ref.symbol.normalized.owner.value, "test.a.")
  }

  test("fromTextDocument") {
    val arg :: Nil = doc.tree.collect {
      case Term.ApplyInfix(_, Term.Name("shouldBe"), _, arg :: Nil) => arg
    }

    assertNotEquals(arg.symbol, Symbol.None)
  }
}
