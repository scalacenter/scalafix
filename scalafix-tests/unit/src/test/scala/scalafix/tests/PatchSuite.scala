package scalafix
package tests

import scala.meta._
import scala.meta.tokens.Token.Ident
import scalafix.testkit.SyntacticRuleSuite
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scalafix.internal.rule.ProcedureSyntax

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
      | }""".stripMargin
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
}
