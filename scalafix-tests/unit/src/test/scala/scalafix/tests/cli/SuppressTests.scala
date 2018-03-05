package scalafix.tests.cli

import scala.meta.{Defn, Lit}
import scalafix.lint.LintCategory
import scalafix.rule.Rule

object SuppressRules {
  val NoVars: Rule = Rule.linter("NoVars") { ctx =>
    val error = LintCategory.error("No vars!")
    ctx.tree.collect {
      case v @ Defn.Var(_, _, _, _) => error.at("no vars", v.pos)
    }
  }

  val NoInts: Rule = Rule.linter("NoInts") { ctx =>
    val error = LintCategory.error("No ints!")
    ctx.tree.collect {
      case i @ Lit.Int(_) => error.at("no ints", i.pos)
    }
  }
}

class SuppressTests extends BaseCliTest {
  checkSuppress(
    name = "suppress vars",
    originalFile = """
                     |object A {
                     |  var a = 1
                     |}
                   """.stripMargin,
    rule = "scala:scalafix.tests.cli.SuppressRules.NoVars",
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
    rule = "scala:scalafix.tests.cli.SuppressRules.NoInts",
    expectedFile = """
                     |object A {
                     |  var a = 1/* scalafix:ok */
                     |}
                   """.stripMargin
  )
}
