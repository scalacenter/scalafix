package scalafix.testkit

import java.nio.charset.StandardCharsets
import org.scalatest.BeforeAndAfterAll
import org.scalatest.exceptions.TestFailedException
import scala.meta._
import scala.meta.internal.io.FileIO
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.testkit.AssertDiff
import scalafix.internal.testkit.CommentAssertion
import scalafix.internal.testkit.EndOfLineAssertExtractor
import scalafix.internal.testkit.MultiLineAssertExtractor
import scalafix.v0.SemanticdbIndex
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite

/** Construct a test suite for running semantic Scalafix rules. */
abstract class SemanticRuleSuite(
    val props: TestkitProperties,
    val isSaveExpect: Boolean
) extends AnyFunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>

  def this(props: TestkitProperties) = this(props, isSaveExpect = false)
  def this() = this(TestkitProperties.loadFromResources())

  @deprecated(
    "Use empty constructor instead. Arguments are passed as resource 'scalafix-testkit.properties'",
    "0.6.0"
  )
  def this(
      index: SemanticdbIndex,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this()

  private def scalaVersion: String = scala.util.Properties.versionNumberString
  private def scalaVersionDirectory: Option[String] =
    if (scalaVersion.startsWith("2.11")) Some("scala-2.11")
    else if (scalaVersion.startsWith("2.12")) Some("scala-2.12")
    else None

  def evaluateTestBody(diffTest: RuleTest): Unit = {
    val (rule, sdoc) = diffTest.run.apply()
    rule.beforeStart()
    val (fixed, messages) =
      try rule.semanticPatch(sdoc, suppress = false)
      finally rule.afterComplete()
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
    val diff = AssertDiff(messages, expectedLintMessages)

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
    test(diffTest.path.testName) {
      evaluateTestBody(diffTest)
    }
  }

  lazy val testsToRun = {
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

object SemanticRuleSuite {
  def defaultClasspath(classDirectory: AbsolutePath) = Classpath(
    classDirectory ::
      RuleCompiler.defaultClasspathPaths.filter(path =>
        path.toNIO.getFileName.toString.contains("scala-library")
      )
  )

  def stripTestkitComments(input: String): String =
    stripTestkitComments(input.tokenize.get)

  def stripTestkitComments(tokens: Tokens): String = {
    val configComment = findTestkitComment(tokens)
    tokens.filter {
      case `configComment` => false
      case EndOfLineAssertExtractor(_) => false
      case MultiLineAssertExtractor(_) => false
      case _ => true
    }.mkString
  }

  def findTestkitComment(tokens: Tokens): Token = {
    tokens
      .find { x =>
        x.is[Token.Comment] && x.syntax.startsWith("/*")
      }
      .getOrElse {
        val input = tokens.headOption.fold("the file")(_.input.path)
        throw new IllegalArgumentException(
          s"Missing /* */ comment at the top of $input"
        )
      }
  }

}
