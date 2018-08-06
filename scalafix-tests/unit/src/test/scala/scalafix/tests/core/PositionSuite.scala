package scalafix.tests.core

import org.scalatest.FunSuite
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scalafix.testkit.DiffAssertions
import scalafix.internal.util.PositionSyntax._

class PositionSuite extends FunSuite with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val marker = "@@".r
      val matches = marker.findAllIn(original).length
      require(matches == 2, "Original must contain two separate '@@'")
      val start = original.indexOf("@@")
      val end = original.lastIndexOf("@@")
      val text = new StringBuilder()
        .append(original.substring(0, start))
        .append(original.substring(start + 2, end))
        .append(original.substring(end + 2))
        .toString
      val input = Input.VirtualFile(name, text)
      val adjustedEnd = end - 2 // adjust for dropped "@@"
      val pos = Position.Range(input, start, adjustedEnd)
      assertNoDiff(pos.formatMessage("", ""), expected)
    }
  }

  check(
    "single-line",
    """
      |object A {
      |  @@val x = 1@@ // this is x
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
      |  @@val x =
      |    1@@ // this is x
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
