package scalafix.tests.core

import scala.meta.inputs.Input
import scala.meta.inputs.Position

import org.scalatest.FunSuite
import scalafix.internal.util.PositionSyntax._
import scalafix.testkit.DiffAssertions

class PositionSuite extends FunSuite with DiffAssertions {

  val startMarker = '→'
  val stopMarker = '←'

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      require(
        original.count(_ == startMarker) == 1,
        s"Original must contain one $startMarker"
      )
      require(
        original.count(_ == stopMarker) == 1,
        s"Original must contain one $stopMarker"
      )
      val start = original.indexOf(startMarker)
      val end = original.indexOf(stopMarker)
      val text = new StringBuilder()
        .append(original.substring(0, start))
        .append(original.substring(start + 1, end))
        .append(original.substring(end + 1))
        .toString
      val input = Input.VirtualFile(name, text)
      val adjustedEnd = end - 1 // adjust for dropped "@@"
      val pos = Position.Range(input, start, adjustedEnd)
      assertNoDiff(pos.formatMessage("", ""), expected)
    }
  }

  check(
    "single-line",
    """
      |object A {
      |  →val x = 1← // this is x
      |}""".stripMargin,
    """single-line:3:3:
      |  val x = 1 // this is x
      |  ^^^^^^^^^
    """.stripMargin
  )

  check(
    "multi-line",
    """
      |object A {
      |  →val x =
      |    1← // this is x
      |}""".stripMargin,
    """multi-line:3:3:
      |  val x =
      |  ^
    """.stripMargin
  )

  test("Position.None") {
    val obtained = Position.None.formatMessage("error", "Boo")
    assert(obtained == "error: Boo")
  }

}
