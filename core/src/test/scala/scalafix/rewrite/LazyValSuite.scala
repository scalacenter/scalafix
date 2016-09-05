package scalafix.rewrite

import scala.meta.inputs.Input
import scalafix.Fixed
import scalafix.util.DiffAssertions

import org.scalatest.FunSuiteLike

class RewriteSuite(rewrite: Rewrite) extends FunSuiteLike with DiffAssertions {

  def rewriteTest(name: String, original: String, expected: String): Unit = {
    test(name) {
      rewrite.rewrite(Input.String(original)) match {
        case Fixed.Success(obtained) =>
          assertNoDiff(obtained, expected)
        case Fixed.Failure(e) =>
          throw e
      }
    }
  }

}

class LazyValSuite extends RewriteSuite(VolatileLazyVal) {

  rewriteTest(
    "basic",
    """|object a {
       |
       |val foo = 1
       |
       |  lazy val x = 2
       |  @volatile lazy val dontChangeMe = 2
       |
       |  class foo {
       |    lazy val z = {
       |      reallyHardStuff()
       |    }
       |  }
       |}
    """.stripMargin,
    """|object a {
       |
       |val foo = 1
       |
       |  @volatile lazy val x = 2
       |  @volatile lazy val dontChangeMe = 2
       |
       |  class foo {
       |    @volatile lazy val z = {
       |      reallyHardStuff()
       |    }
       |  }
       |}
    """.stripMargin
  )
}
