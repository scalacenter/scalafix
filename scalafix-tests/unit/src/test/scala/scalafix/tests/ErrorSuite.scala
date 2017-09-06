package scalafix
package tests

import scala.meta._
import scala.meta.tokens.Token.Ident
import scalafix.testkit.SyntacticRuleSuite
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scalafix.internal.rule.ProcedureSyntax

class ErrorSuite extends SyntacticRuleSuite(ProcedureSyntax) {
  test("on parse error") {
    intercept[ParseException] {
      ProcedureSyntax.apply(Input.String("object A {"))
    }
  }
}
class PatchSuite
    extends SyntacticRuleSuite(Rule.syntactic("PatchSuite")(ctx =>
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
