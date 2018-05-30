package scalafix.tests.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.meta._
import scala.meta.tokens.Token.Ident
import scalafix.v0.Rule
import scalafix.testkit.SyntacticRuleSuite
import scalafix.internal.tests.utils.SkipWindows

class PatchSuite extends SyntacticRuleSuite {

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

  val file = File.createTempFile("foo", ".scala")
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
}
