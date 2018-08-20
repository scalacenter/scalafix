package scalafix.tests.v0

import scala.meta._
import scalafix.tests.core.BaseSemanticSuite

class LegacySyntheticsSuite extends BaseSemanticSuite("LegacySyntheticsTest") {

  test("text") {
    val synthetics = index.synthetics(input)
    val obtained = synthetics.sortBy(_.position.start).mkString("\n")
    val expected =
      """
        |[221..226): *(canBuildFrom[Int])
        |  [0..1): * => _star_.
        |  [2..14): canBuildFrom => scala/collection/immutable/List.canBuildFrom().
        |  [15..18): Int => scala/Int#
        |[221..226): *[List]
        |  [0..1): * => _star_.
        |  [2..6): List => scala/package.List#
        |[230..236): *[Int, Set[Int]]
        |  [0..1): * => _star_.
        |  [2..5): Int => scala/Int#
        |  [7..10): Set => scala/Predef.Set#
        |  [11..14): Int => scala/Int#
        |[245..253): *(canBuildFrom[Int])
        |  [0..1): * => _star_.
        |  [2..14): canBuildFrom => scala/collection/immutable/Set.canBuildFrom().
        |  [15..18): Int => scala/Int#
        |[245..253): *[List[Int], Int, Set[Int]]
        |  [0..1): * => _star_.
        |  [2..6): List => scala/collection/immutable/List#
        |  [7..10): Int => scala/Int#
        |  [13..16): Int => scala/Int#
        |  [18..21): Set => scala/Predef.Set#
        |  [22..25): Int => scala/Int#
        |[268..283): *[Int, List]
        |  [0..1): * => _star_.
        |  [2..5): Int => scala/Int#
        |  [7..11): List => scala/collection/immutable/List#
        |[284..288): *.apply[Future[Int]]
        |  [0..1): * => _star_.
        |  [2..7): apply => scala/collection/immutable/List.apply().
        |  [8..14): Future => scala/concurrent/Future#
        |  [15..18): Int => scala/Int#
        |[289..298): *(global)
        |  [0..1): * => _star_.
        |  [2..8): global => scala/concurrent/ExecutionContext.Implicits.global.
        |[289..295): *.apply[Int]
        |  [0..1): * => _star_.
        |  [2..7): apply => scala/concurrent/Future.apply().
        |  [8..11): Int => scala/Int#
        |[301..309): *(canBuildFrom[Int])
        |  [0..1): * => _star_.
        |  [2..14): canBuildFrom => scala/collection/immutable/List.canBuildFrom().
        |  [15..18): Int => scala/Int#
        |[301..309): *[List[Future[Int]], Int, List[Int]]
        |  [0..1): * => _star_.
        |  [2..6): List => scala/collection/immutable/List#
        |  [7..13): Future => scala/concurrent/Future#
        |  [14..17): Int => scala/Int#
        |  [21..24): Int => scala/Int#
        |  [26..30): List => scala/collection/immutable/List#
        |  [31..34): Int => scala/Int#
      """.stripMargin

    assertNoDiff(obtained, expected)
  }

  test("parsable") {
    val synthetics = index.synthetics(input)
    synthetics.foreach(n => n.text.parse[Term])
  }
}
