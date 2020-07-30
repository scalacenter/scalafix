package scalafix.testkit

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{BeforeAndAfterAll, Suite, TestRegistration}
import scalafix.internal.patch.PatchInternals
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.testkit.{AssertDiff, CommentAssertion}

import scala.meta._
import scala.meta.internal.io.FileIO

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import org.scalatest.TestRegistration
import org.scalatest.exceptions.TestFailedException
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.testkit.AssertDiff
import scalafix.internal.testkit.CommentAssertion

/** Construct a test suite for running semantic Scalafix rules.
 * <p>
 * Mix-in FunSuiteLike (ScalaTest 3.0), AnyFunSuiteLike (ScalaTest 3.1+) or
 * the testing style of your choice if you add your own tests.
 */
abstract class AbstractSemanticRuleSuite(
    val props: TestkitProperties,
    val isSaveExpect: Boolean
) extends Suite
    with TestRegistration
    with DiffAssertions
    with BeforeAndAfterAll { self =>

  def this(props: TestkitProperties) = this(props, isSaveExpect = false)
  def this() = this(TestkitProperties.loadFromResources())

  private def scalaVersion: String = scala.util.Properties.versionNumberString
  private def scalaVersionDirectory: Option[String] =
    if (scalaVersion.startsWith("2.11")) Some("scala-2.11")
    else if (scalaVersion.startsWith("2.12")) Some("scala-2.12")
    else if (scalaVersion.startsWith("2.13")) Some("scala-2.13")
    else None

  def evaluateTestBody(diffTest: RuleTest): Unit = {
    val (rule, sdoc) = diffTest.run.apply()
    rule.beforeStart()
    val res =
      try rule.semanticPatch(sdoc, suppress = false)
      finally rule.afterComplete()
    // verify to verify that tokenPatchApply and fixed are the same
    val fixed =
      PatchInternals.tokenPatchApply(
        res.ruleCtx,
        res.semanticdbIndex,
        res.patches
      )

    assertNoDiff(
      fixed,
      res.fixed,
      "fixed from tokenPatchApply differs from fixed2 from rule.semanticPatch"
    )
    val tokens = fixed.tokenize.get
    val obtained = SemanticRuleSuite.stripTestkitComments(tokens)
    val expected = diffTest.path.resolveOutput(props) match {
      case Right(file) =>
        FileIO.slurp(file, StandardCharsets.UTF_8)
      case Left(err) =>
        if (fixed == sdoc.input.text) {
          // rule is a linter, no need for an output file.
          obtained
        } else {
          fail(err)
        }
    }

    val expectedLintMessages = CommentAssertion.extract(sdoc.tokens)
    val diff = AssertDiff(res.diagnostics, expectedLintMessages)

    if (diff.isFailure) {
      println("###########> Lint       <###########")
      println(diff.toString)
    }
    val result = compareContents(obtained, expected)
    if (result.nonEmpty) {
      println("###########> Diff       <###########")
      println(error2message(obtained, expected))
    }

    val isTestFailure = result.nonEmpty || diff.isFailure
    diffTest.path.resolveOutput(props) match {
      case Right(output) if isTestFailure && isSaveExpect =>
        println(s"promoted expect test: $output")
        Files.write(output.toNIO, obtained.getBytes(StandardCharsets.UTF_8))
      case _ =>
    }

    if (isTestFailure) {
      throw new TestFailedException("see above", 0)
    }
  }

  def runOn(diffTest: RuleTest): Unit = {
    registerTest(diffTest.path.testName) {
      evaluateTestBody(diffTest)
    }
  }

  lazy val testsToRun: List[RuleTest] = {
    val symtab = ClasspathOps.newSymbolTable(props.inputClasspath)
    val classLoader = ClasspathOps.toClassLoader(props.inputClasspath)
    val tests = TestkitPath.fromProperties(props)
    tests.map { test =>
      RuleTest.fromPath(props, test, classLoader, symtab)
    }
  }
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}
