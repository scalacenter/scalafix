package scalafix.tests.v1

import scala.meta._

import scalafix.tests.core.BaseSemanticSuite
import scalafix.v1._

class SymbolSuite extends munit.FunSuite {
  implicit val doc: SemanticDocument =
    BaseSemanticSuite.loadDoc("SymbolTest.scala")

  test("normalized") {
    val ref :: Nil = doc.tree.collect { case Import(Importer(ref, _) :: Nil) =>
      ref
    }

    assertEquals(ref.symbol.normalized.owner.value, "test.a.")
  }

  test("fromTextDocument") {
    val arg :: Nil = doc.tree.collect {
      case Term.ApplyInfix(_, Term.Name("shouldBe"), _, arg :: Nil) => arg
    }

    assertNotEquals(arg.symbol, Symbol.None)
  }

  test("overriddenSymbols") {
    val shouldBe :: Nil = doc.tree.collect {
      case defn: Defn.Def
          if defn.name.value == "shouldBe" && defn.mods
            .exists(mod => mod.is[Mod.Override]) =>
        defn
    }

    assertNotEquals(
      shouldBe.symbol.info.map(_.overriddenSymbols).getOrElse(Nil).length,
      0
    )
  }
}
