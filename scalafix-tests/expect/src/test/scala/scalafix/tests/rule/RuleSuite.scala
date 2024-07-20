package scalafix.tests.rule

import scala.util.control.NonFatal

import org.scalatest.exceptions.TestFailedException
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit._

object RuleSuite {
  def main(args: Array[String]): Unit = {
    if (Array("--save-expect").sameElements(args)) {
      new AbstractSemanticRuleSuite(
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

  // TODO: remove in https://github.com/scalacenter/scalafix/pull/2023
  override def runOn(diffTest: RuleTest): Unit = {
    if (
      buildinfo.RulesBuildInfo.scalaVersion.startsWith("3") &&
      props.scalaVersion.startsWith("2") &&
      diffTest.path.input.toNIO.toString.contains("explicitResultTypes")
    ) return
    else super.runOn(diffTest)
  }

  runAllTests()
}
