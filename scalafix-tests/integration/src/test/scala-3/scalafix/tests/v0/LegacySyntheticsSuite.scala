package scalafix.tests.v0

import scala.meta.*

import scalafix.tests.core.BaseSemanticSuite

class LegacySyntheticsSuite extends BaseSemanticSuite("LegacySyntheticsTest") {

  test("text") {
    val synthetics = index.synthetics(input)
    val obtained = synthetics.sortBy(_.position.start).mkString("\n")
    val expected =
      """
        |[141..142): conv[[A] =>> Any, Int](*)
        |  [0..4): conv => test/LegacySyntheticsTest#conv().
        |  [6..7): A => test/LegacySyntheticsTest#conv().[F][A]
        |  [13..16): Any => scala/Any#
        |  [18..21): Int => scala/Int#
        |  [23..24): * => _star_.
      """.stripMargin

    assertNoDiff(obtained, expected)
  }

  test("parsable") {
    val synthetics = index.synthetics(input)
    synthetics.foreach(n => n.text.parse[Term])
  }
}
