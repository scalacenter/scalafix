package scalafix
package tests

import scala.meta._
import scala.meta.tokens.Token.Ident
import scalafix.patch.TokenPatch
import scalafix.patch.TreePatch
import scalafix.rewrite.ProcedureSyntax
import scalafix.testkit.SyntacticRewriteSuite

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import org.scalameta.logger

class ErrorSuite extends SyntacticRewriteSuite(ProcedureSyntax) {
  test("on parse error") {
    intercept[ParseException] {
      ProcedureSyntax.apply(Input.String("object A {"))
    }
  }
}
class PatchSuite
    extends SyntacticRewriteSuite(Rewrite.syntactic(ctx =>
      ctx.addRight(ctx.tree.tokens.find(_.is[Ident]).get, "bba"))) {

  val original =
    """// Foobar
      |object a {
      |  val x = 2
      |}""".stripMargin

  checkDiff(
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
    Input.LabeledString("/label", original),
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
}
