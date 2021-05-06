package scalafix.tests.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import scala.meta._
import scala.meta.tokens.Token.Ident

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.internal.patch.PatchInternals
import scalafix.internal.tests.utils.SkipWindows
import scalafix.patch.Patch
import scalafix.patch.Patch.internal.Add
import scalafix.patch.Patch.internal.AtomicPatch
import scalafix.patch.Patch.internal.Concat
import scalafix.testkit.AbstractSyntacticRuleSuite
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule

class PatchSuite extends AbstractSyntacticRuleSuite with AnyFunSuiteLike {

  val original: String =
    """// Foobar
      |object a {
      |  val x = 2
      |}""".stripMargin

  case object AddRightRule extends SyntacticRule("addRight") {
    override def fix(implicit doc: SyntacticDocument): Patch = {
      Patch.addRight(doc.tree.tokens.find(_.is[Ident]).get, "bba")
    }
  }

  checkDiff(
    AddRightRule,
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
    AddRightRule,
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
    AddRightRule,
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

  case object AddLeftRule extends SyntacticRule("addRight") {
    override def fix(implicit doc: SyntacticDocument): Patch = {
      Patch.addLeft(doc.tree, "object Foo {}\n")
    }
  }

  check(
    AddLeftRule,
    "addLeft adds the string before the first tree token",
    original,
    """object Foo {}
      |// Foobar
      |object a {
      |  val x = 2
      |}""".stripMargin
  )

  case object AddGlobalImporter extends SyntacticRule("addGlobalImporter") {
    override def fix(implicit doc: SyntacticDocument): Patch = {
      Patch.addGlobalImport(importer"scala.collection.{mutable => _}")
    }
  }
  check(
    AddGlobalImporter,
    "AddGlobalImporter is a syntactic patch",
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
    AddGlobalImporter,
    "AddGlobalImporter skip a comment preceding the first statement",
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

  case object CommentFileRule extends SyntacticRule("commentFileRule") {
    override def fix(implicit doc: SyntacticDocument): Patch = {
      Patch.addAround(doc.tree, "/*", "*/")
    }
  }

  check(
    CommentFileRule,
    "comment a file is a syntactic patch",
    """import a.b
      |
      |object A
      |""".stripMargin,
    """/*import a.b
      |
      |object A
      |*/""".stripMargin
  )

  test("Patch.addAround") {
    val patch = Patch.addAround(tree, "/*", "*/")
    assert(!patch.isEmpty)
    val atomicPatch = PatchInternals.getPatchUnits(patch)
    assert(atomicPatch.length == 1)
    atomicPatch.head match {
      case AtomicPatch(Concat(patch1, patch2)) =>
        assert(patch1.isInstanceOf[Add])
        assert(patch2.isInstanceOf[Add])
      case _ =>
        fail(s"$atomicPatch is not of expected type AtomicPatch(Concat(_, _))")
    }
  }
}
