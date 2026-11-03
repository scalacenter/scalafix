package scalafix.tests.util

import scala.meta._

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.util.ImporterOps
import scalafix.internal.util.ImporterOps._

class ImporterOpsSuite extends AnyFunSuite {

  private def importer(code: String): Importer =
    code.parse[Stat].get.collect { case x: Importer => x }.head

  test("importee without braces") {
    val i = importer("import a.b")
    val b = i.values.head
    assert(ImporterOps.importerOf(b).contains(i))
    assert((i.ref.syntax, i.values.map(_.syntax)) == ("a", List("b")))
  }

  test("importees in braces") {
    val i = importer("import a.{b, c => d}")
    val d = i.values.last
    assert(ImporterOps.importerOf(d).contains(i))
    assert((i.ref.syntax, i.values.map(_.syntax)) == ("a", List("b", "c => d")))
  }

  test("importer from importees") {
    val ref = Term.Name("a")
    val b = Importee.Name(Name("b"))
    val c = Importee.Name(Name("c"))
    assert(ref.importer(b).syntax == "a.b")
    assert(ref.importer(b :: c :: Nil).syntax == "a.{b, c}")
  }

  test("importer with other importees") {
    val i = importer("import a.{b, c}")
    val d = Importee.Name(Name("d"))
    assert(i.withValues(d).syntax == "a.d")
    assert(i.withValues(i.values.last :: d :: Nil).syntax == "a.{c, d}")
  }

  test("importer with another ref") {
    val i = importer("import a.{b, c}")
    val ref = Term.Select(Term.Name("x"), Term.Name("y"))
    assert(i.withRef(ref).syntax == "x.y.{b, c}")
  }

}
