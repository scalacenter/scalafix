package scalafix.tests.core

import scala.meta._

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.internal.tests.utils.SkipWindows
import scalafix.patch.Patch
import scalafix.testkit.AbstractSyntacticRuleSuite
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule

class DialectSuite extends AbstractSyntacticRuleSuite with AnyFunSuiteLike {

  val original: String =
    """|object LiteralType {
      |  val x: 41 = 41
      |}
      |""".stripMargin

  case object LiteralType extends SyntacticRule("LiteralType") {
    override def fix(implicit doc: SyntacticDocument): Patch = {
      doc.tree.collect { case lit @ Lit.Int(n) =>
        Patch.replaceTree(lit, (n + 1).toString)
      }.asPatch
    }
  }

  checkDiff(
    LiteralType,
    Input.String(original),
    """
      |--- Input.String('<object Lit...>')
      |+++ Input.String('<object Lit...>')
      |@@ -1,3 +1,3 @@
      | object LiteralType {
      |-  val x: 41 = 41
      |+  val x: 42 = 42
      | }
      |""".stripMargin,
    SkipWindows
  )

}
