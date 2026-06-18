package scalafix.tests.reflect

import scala.meta.inputs.Input

import metaconfig.Configured
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.reflect.RuleInstrumentation

class RuleInstrumentationSuite extends AnyFunSuite {
  def check(name: String, original: String, expected: List[String]): Unit = {
    test(name) {
      assertResult(Configured.Ok(expected))(
        RuleInstrumentation.getRuleFqn(Input.VirtualFile(name, original))
      )
    }
  }

  check(
    "lenient dialect is supported",
    """
      |package a
      |import scalafix.v0._
      |object MyRule extends Rule("MyRule") {
      |  List(
      |    1,
      |    2,
      |  )
      |}
    """.stripMargin,
    List("a.MyRule")
  )

  // https://github.com/scalacenter/scalafix/issues/2462
  check(
    "companion object without extends",
    """
      |import scalafix.v1._
      |class MyRule extends SyntacticRule("MyRule") {}
      |object MyRule
    """.stripMargin,
    List("MyRule")
  )
}
