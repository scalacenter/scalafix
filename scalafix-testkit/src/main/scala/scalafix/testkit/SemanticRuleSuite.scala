package scalafix
package testkit

import scala.collection.mutable
import scalafix.syntax._
import scala.meta._
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import org.scalameta.logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException
import scala.util.matching.Regex
import scalafix.rule.RuleName
import org.langmeta.internal.ScalafixLangmetaHacks

object SemanticRuleSuite {
  val LintAssertion: Regex = " assert: (.*)".r
  def stripTestkitComments(input: String): String =
    stripTestkitComments(input.tokenize.get)
  def stripTestkitComments(tokens: Tokens): String = {
    val configComment = tokens.find { x =>
      x.is[Token.Comment] && x.syntax.startsWith("/*")
    }.get
    tokens.filter {
      case `configComment` => false
      case Token.Comment(LintAssertion(key)) => false
      case _ => true
    }.mkString
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

  private def dialectToPath(dialect: String): Option[String] =
    Option(dialect).collect {
      case "Scala211" => "scala-2.11"
      case "Scala212" => "scala-2.12"
    }

  private def assertLintMessagesAreReported(
      rule: Rule,
      ctx: RuleCtx,
      patches: Map[RuleName, Patch],
      tokens: Tokens): Unit = {

    type Msg = (Position, String)

    def matches(a: Msg)(b: Msg) =
      a._1.startLine == b._1.startLine &&
        a._2 == b._2

    def diff(a: Seq[Msg], b: Seq[Msg]) =
      a.filter(x => !b.exists(matches(x)))

    val lintAssertions = tokens.collect {
      case tok @ Token.Comment(SemanticRuleSuite.LintAssertion(key)) =>
        tok.pos -> key
    }
    val lintMessages = patches.toSeq.flatMap {
      case (name, patch) =>
        Patch
          .lintMessages(patch, ctx, name)
          .map(lint => lint.position -> lint.category.key(name))
    }

    val uncoveredAsserts = diff(lintAssertions, lintMessages)
    uncoveredAsserts.foreach {
      case (pos, key) =>
        throw new TestFailedException(
          ScalafixLangmetaHacks.formatMessage(
            pos,
            "error",
            s"Message '$key' was not reported here!"),
          0
        )
    }

    val uncoveredMessages = diff(lintMessages, lintAssertions)
    if (uncoveredMessages.nonEmpty) {

      Patch.printLintMessages(patches, ctx)

      val explanation = uncoveredMessages
        .groupBy(_._2)
        .map {
          case (key, positions) =>
            s"""Append to lines: ${positions
                 .map(_._1.startLine)
                 .mkString(", ")}
               |   // assert: $key""".stripMargin
        }
        .mkString("\n\n")
      throw new TestFailedException(
        s"Uncaught linter messages! To fix this problem\n$explanation",
        0)
    }
  }

  def runOn(diffTest: DiffTest): Unit = {
    test(diffTest.name) {
      val (rule, config) = diffTest.config.apply()
      val ctx: RuleCtx = RuleCtx(
        config.dialect(diffTest.original).parse[Source].get,
        config.copy(dialect = diffTest.document.dialect)
      )
      val patches = rule.fixWithName(ctx)
      assertLintMessagesAreReported(rule, ctx, patches, ctx.tokens)
      val patch = patches.values.asPatch
      val obtainedWithComment = Patch.apply(patch, ctx, rule.semanticOption)
      val tokens = obtainedWithComment.tokenize.get
      val obtained = SemanticRuleSuite.stripTestkitComments(tokens)
      val candidateOutputFiles = expectedOutputSourceroot.flatMap { root =>
        val scalaSpecificFilename =
          dialectToPath(diffTest.document.language).toList.map(path =>
            root.resolve(RelativePath(
              diffTest.filename.toString().replaceFirst("scala", path))))
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
      assertNoDiff(obtained, expected)
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
