package scalafix.testkit

import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
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

/** Construct a test suite for running semantic Scalafix rules. */
abstract class SemanticRuleSuite
    extends FunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>

  @deprecated(
    "Use empty constructor instead. Arguments are passed as resource 'scalafix-testkit.properties'",
    "0.6.0")
  def this(
      index: SemanticdbIndex,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this()

  val props: TestkitProperties = TestkitProperties.loadFromResources()
  private def scalaVersion: String = scala.util.Properties.versionNumberString
  private def scalaVersionDirectory: Option[String] =
    if (scalaVersion.startsWith("2.11")) Some("scala-2.11")
    else if (scalaVersion.startsWith("2.12")) Some("scala-2.12")
    else None

  def runOn(diffTest: RuleTest): Unit = {
    test(diffTest.filename.toString()) {
      val (rule, sdoc) = diffTest.run.apply().get
      val (fixed, messages) = rule.semanticPatch(sdoc, suppress = false)

      val tokens = fixed.tokenize.get
      val obtained = SemanticRuleSuite.stripTestkitComments(tokens)
      val candidateOutputFiles = props.outputSourceDirectories.flatMap { root =>
        val scalaSpecificFilename = scalaVersionDirectory.toList.map { path =>
          root.resolve(
            RelativePath(
              diffTest.filename.toString().replaceFirst("scala", path)))
        }
        root.resolve(diffTest.filename) :: scalaSpecificFilename
      }
      val expected = candidateOutputFiles
        .collectFirst {
          case f if f.isFile =>
            FileIO.slurp(f, StandardCharsets.UTF_8)
        }
        .getOrElse {
          if (fixed == sdoc.input.text) {
            obtained // linter
          } else {
            val tried = candidateOutputFiles.mkString("\n")
            sys.error(
              s"""Missing expected output file for test ${diffTest.filename}. Tried:
                 |$tried""".stripMargin)
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

      if (result.nonEmpty || diff.isFailure) {
        throw new TestFailedException("see above", 0)
      }
    }
  }

  lazy val testsToRun = {
    val symtab = ClasspathOps
      .newSymbolTable(props.inputClasspath)
      .getOrElse { sys.error("Failed to load symbol table") }
    val classLoader = ClasspathOps.toClassLoader(props.inputClasspath)
    props.inputSourceDirectories.flatMap { dir =>
      RuleTest.fromDirectory(props.inputOffset, dir, classLoader, symtab)
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
      path.toNIO.getFileName.toString.contains("scala-library"))
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
        val input = tokens.headOption.fold("the file")(_.input.toString)
        throw new IllegalArgumentException(
          s"Missing /* */ comment at the top of $input")
      }
  }

}
