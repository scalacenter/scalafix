package scalafix.testkit

import java.nio.charset.StandardCharsets
import scala.meta.internal.io.FileIO
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException
import scala.meta._
import scalafix.internal.reflect.ClasspathOps

object SemanticRuleSuite {

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

abstract class SemanticRuleSuite(
    val sourceroot: AbsolutePath,
    val classpath: Classpath,
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends FunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>

  def scalaVersion = scala.util.Properties.versionNumberString
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
      val candidateOutputFiles = expectedOutputSourceroot.flatMap { root =>
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
      .newSymbolTable(classpath)
      .getOrElse { sys.error("Failed to load symbol table") }
    RuleTest.fromDirectory(sourceroot, classpath, symtab)
  }
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}
