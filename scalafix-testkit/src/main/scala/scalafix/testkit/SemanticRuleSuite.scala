package scalafix
package testkit

import scalafix.syntax._
import scala.meta._

import scalafix.internal.util.EagerInMemorySemanticdbIndex
import org.scalameta.logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException

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
    val index: SemanticdbIndex,
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends FunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>
  def this(
      index: SemanticdbIndex,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this(
    index,
    expectedOutputSourceroot
  )
  def this(
      database: Database,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) =
    this(
      EagerInMemorySemanticdbIndex(
        database,
        Sourcepath(inputSourceroot),
        Classpath(Nil)),
      inputSourceroot,
      expectedOutputSourceroot
    )

  def scalaVersion = scala.util.Properties.versionNumberString
  private def scalaVersionDirectory: Option[String] =
    if (scalaVersion.startsWith("2.11")) Some("scala-2.11")
    else if (scalaVersion.startsWith("2.12")) Some("scala-2.12")
    else None

  def runOn(diffTest: DiffTest): Unit = {
    test(diffTest.name) {
      val (rule, config) = diffTest.config.apply()
      val ctx: RuleCtx = RuleCtx(
        config.dialect(diffTest.original).parse[Source].get,
        config.copy(dialect = diffTest.document.dialect)
      )
      val patches = rule.fixWithName(ctx)
      val (obtainedWithComment, obtainedLintMessages) =
        Patch.apply(patches, ctx, rule.semanticOption)

      val patch = patches.values.asPatch
      val tokens = obtainedWithComment.tokenize.get
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
        .collectFirst { case f if f.isFile => f.readAllBytes }
        .map(new String(_))
        .getOrElse {
          if (Patch.isOnlyLintMessages(patch)) obtained // linter
          else {
            val tried = candidateOutputFiles.mkString("\n")
            sys.error(
              s"""Missing expected output file for test ${diffTest.filename}. Tried:
                 |$tried""".stripMargin)
          }
        }

      if (!Patch.isAllTokenPatchAtomic(patch))
        sys.error(
          s"One or more rules used by test ${diffTest.filename} emit non-atomic " +
            "top level patches. To fix this, please use `.atomic` on individual or group " +
            "of patches that need to be atomically applied.")

      val expectedLintMessages = CommentAssertion.extract(ctx.tokens)
      val diff = AssertDiff(obtainedLintMessages, expectedLintMessages)

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

  /** Helper method to print out index for individual files */
  def debugFile(filename: String): Unit = {
    index.documents.foreach { entry =>
      if (entry.input.label.contains(filename)) {
        logger.elem(entry)
      }
    }
  }

  override def afterAll(): Unit = {
    val onlyTests = testsToRun.filter(_.isOnly).toList
    if (sys.env.contains("CI") && onlyTests.nonEmpty) {
      sys.error(
        s"sys.env('CI') is set and the following tests are marked as ONLY: " +
          s"${onlyTests.map(_.filename).mkString(", ")}")
    }
    super.afterAll()
  }
  lazy val testsToRun =
    DiffTest.testToRun(DiffTest.fromSemanticdbIndex(index))
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}
