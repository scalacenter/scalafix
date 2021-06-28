package scalafix.tests.rule

import scala.util.control.NonFatal

import org.scalatest.exceptions.TestFailedException
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit._

object RuleSuite {
  def main(args: Array[String]): Unit = {
    if (Array("--save-expect").sameElements(args)) {
      val suite = new AbstractSemanticRuleSuite(
        TestkitProperties.loadFromResources(),
        isSaveExpect = true
      ) with AnyFunSuiteLike {
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
class RuleSuite extends AbstractSemanticRuleSuite with AnyFunSuiteLike {
//  runAllTests()
  runSpecificTests("scala3NewSyntax")
}
