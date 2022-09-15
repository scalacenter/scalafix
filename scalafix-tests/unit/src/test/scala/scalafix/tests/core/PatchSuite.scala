package scalafix.tests.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import scala.meta._
import scala.meta.internal.prettyprinters.TreeSyntax
import scala.meta.tokens.Token.Ident

import org.scalatest.FunSuiteLike
import scalafix.internal.patch.PatchInternals
import scalafix.internal.tests.utils.SkipWindows
import scalafix.patch.Patch
import scalafix.patch.Patch.internal.Add
import scalafix.patch.Patch.internal.AtomicPatch
import scalafix.patch.Patch.internal.Concat
import scalafix.testkit.AbstractSyntacticRuleSuite
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule

class PatchSuite extends AbstractSyntacticRuleSuite with FunSuiteLike {

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

  case class ReplaceTreeRule(pf: PartialFunction[Tree, String])
      extends SyntacticRule("replaceTreeRule") {

    override def fix(implicit doc: SyntacticDocument): Patch = {
      val patch = doc.tree.collect {
        case t if pf.isDefinedAt(t) => Patch.replaceTree(t, pf(t))
      }.asPatch
      assert(patch.nonEmpty)
      patch
    }
  }

  val program: Source =
    """import a.b
      |
      |class Foo(private val q: String = "") extends Bar with Qux {
      |  override def toString = if (true) return "hello" else ???
      |  lazy val x: Seq[_] = List(2)
      |  foo[Bar](null)
      |  new R()
      |  object O {}
      |}""".stripMargin.parse[Source].get

  program
    .collect { case t => t }
    .filterNot(_.pos.text == "")
    .groupBy(_.productPrefix)
    .map(_._2.head)
    .foreach { from =>
      val replace: PartialFunction[Tree, String] = {
        case t if t.structure == from.structure =>
          TreeSyntax.reprint(t)(implicitly[Dialect]).toString
      }
      checkDiff(
        ReplaceTreeRule(replace),
        Input.VirtualFile(
          s"Patch.replaceTree(reprint(${from.productPrefix}))",
          TreeSyntax.reprint(program)(implicitly[Dialect]).toString
        ),
        "" // replacing a tree by its syntax should be a no-op
      )
    }
}
