package scalafix.tests.rule

import scala.util.control.NonFatal

import buildinfo.RulesBuildInfo
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

  override def runOn(diffTest: RuleTest): Unit = {
    def stripPatch(v: String) = v.split('.').take(2).mkString(".")

    val versionMismatch =
      stripPatch(RulesBuildInfo.scalaVersion) != stripPatch(props.scalaVersion)
    val explicitResultTypesTest =
      diffTest.path.input.toNIO.toString.contains("explicitResultTypes")

    // ExplicitResultTypes can only run against sources compiled with the sam
    // binary version as the one used to compile the rule
    if (versionMismatch && explicitResultTypesTest) return
    else super.runOn(diffTest)
  }

  runAllTests()
}
