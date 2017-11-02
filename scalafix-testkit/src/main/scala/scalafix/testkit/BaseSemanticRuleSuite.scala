package scalafix.testkit

import java.io.File
import scala.meta._
import scala.util.matching.Regex
import scalafix.Patch
import scalafix.Rule
import scalafix.RuleCtx
import scalafix.SemanticdbIndex
import scalafix.internal.testkit.DiffTest
import scalafix.syntax._
import scalafix.rule.RuleName
import org.langmeta.internal.ScalafixLangmetaHacks
import org.scalameta.logger

trait BaseSemanticRuleSuite extends BaseScalafixSuite { self =>
  def index: SemanticdbIndex
  def expectedOutputSourceroot: Seq[AbsolutePath]

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
      case tok @ Token.Comment(BaseSemanticRuleSuite.LintAssertion(key)) =>
        tok.pos -> key
    }
    val lintMessages = patches.toSeq.flatMap {
      case (name, patch) =>
        Patch
          .lintMessages(patch)
          .map(lint => lint.position -> lint.category.key(name))
    }

    val uncoveredAsserts = diff(lintAssertions, lintMessages)
    uncoveredAsserts.foreach {
      case (pos, key) =>
        throw new Exception(
          ScalafixLangmetaHacks.formatMessage(
            pos,
            "error",
            s"Message '$key' was not reported here!")
        )
    }

    val uncoveredMessages = diff(lintMessages, lintAssertions)
    if (uncoveredMessages.nonEmpty) {
      Patch.reportLintMessages(patches, ctx)
      val explanation = uncoveredMessages
        .groupBy(_._2)
        .map {
          case (key, positions) =>
            s"""Append to lines: ${positions.map(_._1.startLine).mkString(", ")}
               |   // assert: $key""".stripMargin
        }
        .mkString("\n\n")
      throw new Fail(
        s"Uncaught linter messages! To fix this problem\n$explanation"
      )
    }
  }

  def runOn(diffTest: DiffTest): Unit = {
    val suffix = "scala/test/".replace('/', File.separatorChar)
    scalafixTest(diffTest.name.stripPrefix(suffix).stripSuffix(".scala")) {
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
      val obtained = BaseSemanticRuleSuite.stripTestkitComments(tokens)
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

  lazy val testsToRun: Seq[DiffTest] = DiffTest.fromSemanticdbIndex(index)
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}

object BaseSemanticRuleSuite {
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
