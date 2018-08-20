package scalafix.tests.v0

import scala.meta._
import scalafix.tests.core.BaseSemanticSuite

class LegacyDenotationSuite extends BaseSemanticSuite("LegacyDenotationTest") {
  test("convert methods") {
    object TestDenotation {
      private val denotations = Map(
        "cmap" ->
          """|private val method cmap: Map[Int, Int]
             |  [0..3): Map => scala/collection/Map#
             |  [4..7): Int => scala/Int#
             |  [9..12): Int => scala/Int#""".stripMargin,
        "cset" ->
          """|private val method cset: Set[Int]
             |  [0..3): Set => scala/collection/Set#
             |  [4..7): Int => scala/Int#""".stripMargin,
        "imap" ->
          """|private val method imap: Map[Int, Int]
             |  [0..3): Map => scala/collection/immutable/Map#
             |  [4..7): Int => scala/Int#
             |  [9..12): Int => scala/Int#""".stripMargin,
        "iset" ->
          """|private val method iset: Set[Int]
             |  [0..3): Set => scala/collection/immutable/Set#
             |  [4..7): Int => scala/Int#""".stripMargin
      )
      def unapply(n: Term.Name): Option[String] = denotations.get(n.syntax)
    }
    val converted = source
      .collect {
        case tree @ TestDenotation(expected) =>
          index
            .denotation(tree)
            .map(obtained => (obtained.toString, expected))
      }
      .flatten
      .distinct

    val (obtained, expected) = converted.unzip

    def show(xs: List[String]): String = xs.mkString("\n")

    assertNoDiff(show(obtained), show(expected))
  }
}
