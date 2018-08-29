package scalafix.tests.util

import scala.meta._
import org.scalatest.FunSuite
import scalafix.testkit.DiffAssertions
import scalafix.v1._

class ScalametaStructureSuite extends FunSuite with DiffAssertions {
  test("pretty(t)") {
    val obtained = q"a.b.c.d".structure(1)
    val expected =
      """|Term.Select(
         |  Term.Select(
         |    Term.Select(
         |      Term.Name("a"),
         |      Term.Name("b")
         |    ),
         |    Term.Name("c")
         |  ),
         |  Term.Name("d")
         |)
         |""".stripMargin
    assertNoDiff(obtained, expected)
  }

  test("pretty(t, showFieldNames = true)") {
    val obtained = q"a.b.c.d".structureLabeled(1)
    val expected =
      """|
         |Term.Select(
         |  qual = Term.Select(
         |    qual = Term.Select(
         |      qual = Term.Name("a"),
         |      name = Term.Name("b")
         |    ),
         |    name = Term.Name("c")
         |  ),
         |  name = Term.Name("d")
         |)
         |""".stripMargin
    assertNoDiff(obtained, expected)
  }

  test("option") {
    assertNoDiff(
      q"def foo: A = ???".decltpe.structure(1),
      """|
         |Some(Type.Name("A"))
         |""".stripMargin
    )
  }

  test("list") {
    assertNoDiff(
      q"foo(a)".args.structure(1),
      """|List(
         |  Term.Name("a")
         |)
         |""".stripMargin
    )
  }
}
