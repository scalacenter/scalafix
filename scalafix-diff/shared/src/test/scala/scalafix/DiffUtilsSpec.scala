package scalafix.diff

import org.scalatest.FunSuite

class DiffUtilsSpec extends FunSuite {

  val originalFileName = "SomeFile.scala"
  val revisedFileName = "SomeFileRenamed.scala"
  val originalLines =
    s"""|object Foo {
        |  const x = Map(
        |    'a -> "a",
        |    'b -> "b",
        |    'c -> "c"
        |  )
        |}""".stripMargin.split("\n").toList
  val revisedLines =
    s"""|object Foo {
        |  const y = Map(
        |    'd -> "d",
        |    'a -> "a",
        |    'b -> "b",
        |    'c -> "c",
        |    'e -> "e"
        |  )
        |}""".stripMargin.split("\n").toList

  val contextSize = 4

  val expectedDiff =
    s"""|--- SomeFile.scala
        |+++ SomeFileRenamed.scala
        |@@ -1,7 +1,9 @@
        | object Foo {
        |-  const x = Map(
        |+  const y = Map(
        |+    'd -> "d",
        |     'a -> "a",
        |     'b -> "b",
        |-    'c -> "c"
        |+    'c -> "c",
        |+    'e -> "e"
        |   )
        | }""".stripMargin

  test("unifiedDiff") {
    val actualDiff = DiffUtils.unifiedDiff(originalFileName, revisedFileName, originalLines, revisedLines, contextSize)
    assert(actualDiff == expectedDiff)
  }

}
