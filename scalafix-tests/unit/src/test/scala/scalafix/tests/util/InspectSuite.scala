package scalafix.tests.util

import scala.meta._
import org.scalatest.FunSuite
import scalafix.testkit.DiffAssertions
import scalafix.v1._

class InspectSuite extends FunSuite with DiffAssertions {
  test("pretty(t)") {
    val obtained = q"a.b.c.d".inspect
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
         |)""".stripMargin
    assert(obtained == expected)
  }

  test("pretty(t, showFieldNames = true)") {
    val obtained = q"a.b.c.d".inspectLabeled
    val expected =
      """|Term.Select(
         |  qual = Term.Select(
         |    qual = Term.Select(
         |      qual = Term.Name("a"),
         |      name = Term.Name("b")
         |    ),
         |    name = Term.Name("c")
         |  ),
         |  name = Term.Name("d")
         |)""".stripMargin
    assert(obtained == expected)
  }

  test("option") {
    assertNoDiff(
      q"def foo: A = ???".decltpe.inspect,
      """|
         |Some(Type.Name("A"))
         |""".stripMargin
    )
  }

  test("list") {
    assertNoDiff(
      q"foo(a)".args.inspect,
      """|List(
         |  Term.Name("a")
         |)
         |""".stripMargin
    )
  }
}
