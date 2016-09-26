package scalafix.rewrite

import scalafix.Failure
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.util.DiffAssertions

import org.scalatest.FunSuite

class ProcedureSyntaxSuite extends RewriteSuite(ProcedureSyntax) {

  check(
    "nested function",
    """
      |import /* a */ a.b.c
      |import a.b.c
      |// This is a comment
      |@annotation
      |object Main {
      |  def main(args: Seq[String]) {
      |  var number = 2
      |    def increment(n: Int) {
      |      number += n
      |    }
      |    increment(3)
      |      args.foreach(println(number))
      |  }
      |}""".stripMargin,
    """
      |import /* a */ a.b.c
      |import a.b.c
      |// This is a comment
      |@annotation
      |object Main {
      |  def main(args: Seq[String]): Unit = {
      |  var number = 2
      |    def increment(n: Int): Unit = {
      |      number += n
      |    }
      |    increment(3)
      |      args.foreach(println(number))
      |  }
      |}""".stripMargin
  )
  check(
    "no right paren",
    """
      |object a {
      |def foo {
      |  println(1)
      |}
      |}
    """.stripMargin,
    """
      |object a {
      |def foo: Unit = {
      |  println(1)
      |}
      |}
    """.stripMargin
  )

  check(
    "pathological comment",
    """
      |object a {
      |def main() /* unit */ {
      |}}
    """.stripMargin,
    """
      |object a {
      |def main(): Unit = { /* unit */
      |}}
  """.stripMargin
  )
  test("on parse error") {
    val Left(err) = Scalafix.fix("object A {")
    assert(err.isInstanceOf[Failure.ParseError])
  }

}
