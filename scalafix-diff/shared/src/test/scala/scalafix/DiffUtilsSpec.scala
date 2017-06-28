package scalafix.diff

import org.scalatest.FunSuite

class DiffUtilsTest extends FunSuite {

  val originalFileName = "SomeFile.scala"
  val revisedFileName = "SomeFileRenamed.scala"
  val originalLines =
    s"""|object Foo {
        |  val a = "foo"
        |  val b = 42
        |  case class Bar(x: Boolean)
        |  val x = Map(
        |    'a -> "a",
        |    'b -> "b",
        |    'c -> "c"
        |  )
        |  val c = 1 + 1
        |  val d = true
        |}""".stripMargin.split("\n").toList
  val revisedLines =
    s"""|object Foo {
        |  val a = "foo"
        |  val b = 42
        |  case class Bar(x: Boolean)
        |  val y = Map(
        |    'd -> "d",
        |    'a -> "a",
        |    'b -> "b",
        |    'c -> "c",
        |    'e -> "e"
        |  )
        |  val c = 1 + 1
        |  val d = true
        |}""".stripMargin.split("\n").toList

  val contextSize = 2

  val expectedDiff =
    s"""|--- SomeFile.scala
        |+++ SomeFileRenamed.scala
        |@@ -3,8 +3,10 @@
        |   val b = 42
        |   case class Bar(x: Boolean)
        |-  val x = Map(
        |+  val y = Map(
        |+    'd -> "d",
        |     'a -> "a",
        |     'b -> "b",
        |-    'c -> "c"
        |+    'c -> "c",
        |+    'e -> "e"
        |   )
        |   val c = 1 + 1""".stripMargin

  test("unifiedDiff") {
    val actualDiff = DiffUtils.unifiedDiff(originalFileName,
                                           revisedFileName,
                                           originalLines,
                                           revisedLines,
                                           contextSize)
    assert(actualDiff == expectedDiff)
  }

}
