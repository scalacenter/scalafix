package scalafix

import scalafix.rewrite.ProcedureSyntax

import org.scalatest.FunSuite

class ScalafixTest extends FunSuite {
  val rewrites = Seq(ProcedureSyntax)

  def testInput(name: String, input: String, expected: String): Unit = {
    test(name) {
      val obtained = Scalafix.fix(input, rewrites)
      assert(obtained.trim === expected.trim)
    }
  }

  testInput(
      "nested function",
      """
        | import a.b.c
        | import a.b.c
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
        |import a.b.c
        |import a.b.c
        |object Main {
        |  def main(args: Seq[String]): Unit = {
        |    var number = 2
        |    def increment(n: Int): Unit = {
        |      number += n
        |    }
        |    increment(3)
        |    args.foreach(println(number))
        |  }
        |}""".stripMargin
  )

}
