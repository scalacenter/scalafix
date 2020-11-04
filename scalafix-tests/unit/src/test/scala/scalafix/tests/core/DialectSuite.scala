package scalafix.tests.core

import scala.meta._

import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.internal.tests.utils.SkipWindows
import scalafix.testkit.AbstractSyntacticRuleSuite
import scalafix.v0.Rule

class DialectSuite extends AbstractSyntacticRuleSuite with AnyFunSuiteLike {

  val original: String =
    """|object LiteralType {
      |  val x: 41 = 41
      |}
      |""".stripMargin

  val LiteralType: Rule = Rule.syntactic("LiteralType") { ctx =>
    ctx.tree.collect { case lit @ Lit.Int(n) =>
      ctx.replaceTree(lit, (n + 1).toString)
    }.asPatch
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
