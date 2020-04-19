package scalafix.tests.v1

import org.scalatest.FunSuite
import scala.meta.Import
import scala.meta.Importer
import scalafix.tests.core.BaseSemanticSuite

class SymbolSuite extends FunSuite {
  implicit val doc = BaseSemanticSuite.loadDoc("SymbolTest.scala")

  test("normalized") {
    val ref :: Nil = doc.tree.collect {
      case Import(Importer(ref, _) :: Nil) => ref
    }

    import scalafix.v1._
    assert(ref.symbol.normalized.owner.value == "test.a.")
  }
}
