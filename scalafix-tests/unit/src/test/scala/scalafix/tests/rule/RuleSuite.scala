package scalafix.tests.rule

import scalafix.testkit._
import scala.util.control.NonFatal
import org.scalatest.exceptions.TestFailedException

object RuleSuite {
  def main(args: Array[String]): Unit = {
    if (Array("--save-expect").sameElements(args)) {
      val suite = new SemanticRuleSuite(
        TestkitProperties.loadFromResources(),
        isSaveExpect = true
      ) {
        testsToRun.foreach { t =>
          try evaluateTestBody(t)
          catch {
            case _: TestFailedException =>
            case NonFatal(e) =>
              e.printStackTrace()
          }
        }
      }
      println("Promoted expect tests")
    } else {
      println(
        s"unknown arguments '${args.mkString(" ")}', expected '--save-expect'"
      )
    }
  }
}
class RuleSuite extends SemanticRuleSuite {
  runAllTests()
}
