package scalafix.tests.cli

import scala.meta.Defn
import scala.meta.Lit

import scalafix.patch.Patch
import scalafix.v0.LintCategory
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule

class NoVars extends SyntacticRule("NoVars") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    val error = LintCategory.error("No vars!")
    doc.tree.collect { case v @ Defn.Var.After_4_7_2(_, _, _, _) =>
      Patch.lint(error.at("no vars", v.pos))
    }.asPatch
  }
}

class NoInts extends SyntacticRule("NoInts") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    val error = LintCategory.error("No ints!")
    doc.tree.collect { case i @ Lit.Int(_) =>
      Patch.lint(error.at("no ints", i.pos))
    }.asPatch
  }
}

class CliSuppressSuite extends BaseCliSuite {
  checkSuppress(
    name = "suppress vars",
    originalFile = """
      |object A {
      |  var a = 1
      |}
                   """.stripMargin,
    rule = "scala:scalafix.tests.cli.NoVars",
    expectedFile = """
      |object A {
      |  var/* scalafix:ok */ a = 1
      |}
                   """.stripMargin
  )

  checkSuppress(
    name = "suppress int",
    originalFile = """
      |object A {
      |  var a = 1
      |}
                   """.stripMargin,
    rule = "scala:scalafix.tests.cli.NoInts",
    expectedFile = """
      |object A {
      |  var a = 1/* scalafix:ok */
      |}
                   """.stripMargin
  )
}
