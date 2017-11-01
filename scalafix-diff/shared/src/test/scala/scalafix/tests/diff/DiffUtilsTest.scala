package scalafix.tests.diff
import utest._

object DiffUtilsTest extends TestSuite {

  override def tests: Tests = Tests {
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

    "unifiedDiff" - {
      val actualDiff = scalafix.diff.DiffUtils.unifiedDiff(
        originalFileName,
        revisedFileName,
        originalLines,
        revisedLines,
        contextSize)
      assert(actualDiff == expectedDiff)
    }

  }
}
