package scalafix.tests.core

import scala.meta._
import System.{lineSeparator => nl}

class SyntheticSuite extends BaseSemanticSuite("SyntheticTest") {

  test("text") {
    val synthetics = index.synthetics(input)
    val obtained = synthetics.sortBy(_.position.start).mkString(nl)
    val expected =
      """|[200..205): *(canBuildFrom[Int])
         |  [0..1): * => _star_.
         |  [2..14): canBuildFrom => scala/collection/immutable/List.canBuildFrom().
         |  [15..18): Int => scala/Int#
         |[200..205): *[List]
         |  [0..1): * => _star_.
         |  [2..6): List => scala/package.List#
         |[209..215): *[Int, Set[Int]]
         |  [0..1): * => _star_.
         |  [2..5): Int => scala/Int#
         |  [7..10): Set => scala/Predef.Set#
         |  [11..14): Int => scala/Int#
         |[224..232): *(canBuildFrom[Int])
         |  [0..1): * => _star_.
         |  [2..14): canBuildFrom => scala/collection/immutable/Set.canBuildFrom().
         |  [15..18): Int => scala/Int#
         |[224..232): *[List[Int], Int, Set[Int]]
         |  [0..1): * => _star_.
         |  [2..6): List => scala/collection/immutable/List#
         |  [7..10): Int => scala/Int#
         |  [13..16): Int => scala/Int#
         |  [18..21): Set => scala/Predef.Set#
         |  [22..25): Int => scala/Int#
         |[247..262): *[Int, List]
         |  [0..1): * => _star_.
         |  [2..5): Int => scala/Int#
         |  [7..11): List => scala/collection/immutable/List#
         |[263..267): *.apply[Future[Int]]
         |  [0..1): * => _star_.
         |  [2..7): apply => scala/collection/immutable/List.apply().
         |  [8..14): Future => scala/concurrent/Future#
         |  [15..18): Int => scala/Int#
         |[268..277): *(global)
         |  [0..1): * => _star_.
         |  [2..8): global => scala/concurrent/ExecutionContext.Implicits.global.
         |[268..274): *.apply[Int]
         |  [0..1): * => _star_.
         |  [2..7): apply => scala/concurrent/Future.apply().
         |  [8..11): Int => scala/Int#
         |[280..288): *(canBuildFrom[Int])
         |  [0..1): * => _star_.
         |  [2..14): canBuildFrom => scala/collection/immutable/List.canBuildFrom().
         |  [15..18): Int => scala/Int#
         |[280..288): *[List[Future[Int]], Int, List[Int]]
         |  [0..1): * => _star_.
         |  [2..6): List => scala/collection/immutable/List#
         |  [7..13): Future => scala/concurrent/Future#
         |  [14..17): Int => scala/Int#
         |  [21..24): Int => scala/Int#
         |  [26..30): List => scala/collection/immutable/List#
         |  [31..34): Int => scala/Int#""".stripMargin

    assertNoDiff(obtained, expected)
  }

  test("parsable") {
    val synthetics = index.synthetics(input)
    synthetics.foreach(n => n.text.parse[Term])
  }
}
