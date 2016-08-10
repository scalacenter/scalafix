package scalafix

import scalafix.rewrite.ProcedureSyntax
import scalafix.util.DiffAssertions

import org.scalatest.FunSuite

class ScalafixSuite extends FunSuite with DiffAssertions {
  val rewrites = Seq(ProcedureSyntax)

  def testInput(name: String, input: String, expected: String): Unit = {
    test(name) {
      val obtained = Scalafix.fix(input, rewrites)
      assertNoDiff(obtained.get, expected)
    }
  }

  testInput(
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

  test("on parse error") {
    val obtained = Scalafix.fix("object A {")
    assert(obtained.isInstanceOf[FixResult.ParseError])
  }

}
