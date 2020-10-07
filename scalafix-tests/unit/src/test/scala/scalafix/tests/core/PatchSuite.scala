package scalafix.tests.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import scala.meta._
import scala.meta.tokens.Token.Ident

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.internal.tests.utils.SkipWindows
import scalafix.patch.Patch
import scalafix.testkit.AbstractSyntacticRuleSuite
import scalafix.v0.Rule

class PatchSuite extends AbstractSyntacticRuleSuite with AnyFunSuiteLike {

  val original: String =
    """// Foobar
      |object a {
      |  val x = 2
      |}""".stripMargin

  val addRightRule: Rule = Rule.syntactic("addRight") { (ctx) =>
    ctx.addRight(ctx.tree.tokens.find(_.is[Ident]).get, "bba")
  }

  checkDiff(
    addRightRule,
    Input.String(original),
    """--- Input.String('<// Foobar...>')
      |+++ Input.String('<// Foobar...>')
      |@@ -1,4 +1,4 @@
      | // Foobar
      |-object a {
      |+object abba {
      |   val x = 2
      | }""".stripMargin,
    SkipWindows
  )

  checkDiff(
    addRightRule,
    Input.VirtualFile("/label", original),
    """--- /label
      |+++ /label
      |@@ -1,4 +1,4 @@
      | // Foobar
      |-object a {
      |+object abba {
      |   val x = 2
      | }""".stripMargin
  )

  val file: File = File.createTempFile("foo", ".scala")
  Files.write(Paths.get(file.toURI), original.getBytes)
  checkDiff(
    addRightRule,
    Input.File(file),
    s"""--- ${file.getAbsolutePath}
       |+++ ${file.getAbsolutePath}
       |@@ -1,4 +1,4 @@
       | // Foobar
       |-object a {
       |+object abba {
       |   val x = 2
       | }""".stripMargin
  )

  val addLeftRule: Rule = Rule.syntactic("addLeft") { (ctx) =>
    ctx.addLeft(ctx.tree, "object Foo {}\n")
  }

  check(
    addLeftRule,
    "addLeft adds the string before the first tree token",
    original,
    """object Foo {}
      |// Foobar
      |object a {
      |  val x = 2
      |}""".stripMargin
  )

  val addGlobalImporter: Rule = Rule.syntactic("addGlobalImporter") { ctx =>
    ctx.addGlobalImport(importer"scala.collection.{mutable => _}")
  }

  check(
    addGlobalImporter,
    "addGlobalImporter is a syntactic patch",
    """import a.b
      |
      |object A
      |""".stripMargin,
    """import a.b
      |import scala.collection.{ mutable => _ }
      |
      |object A
      |""".stripMargin
  )

  check(
    addGlobalImporter,
    "addGlobalImporter skip a comment preceding the first statement",
    """package foo.bar
      |
      |/**
      | * API docs here
      | */
      |object A
      |""".stripMargin,
    """package foo.bar
      |
      |import scala.collection.{ mutable => _ }
      |/**
      | * API docs here
      | */
      |object A
      |""".stripMargin
  )

  val tree: Source = Input.String(original).parse[Source].get

  test("Patch.empty") {
    val empty = Patch.empty
    assert(empty.isEmpty)
    assert(empty.atomic.isEmpty)
    assert((empty + empty).isEmpty)
    assert((empty + empty).atomic.isEmpty)

    val nonEmpty = Patch.addLeft(tree, "?")
    assert(nonEmpty.isEmpty == false)
    assert(nonEmpty.atomic.isEmpty == false)
    assert((nonEmpty + nonEmpty).isEmpty == false)
    assert((nonEmpty + nonEmpty).atomic.isEmpty == false)
  }
}
