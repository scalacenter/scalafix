package scalafix
package testkit

import scala.collection.mutable
import scalafix.syntax._
import scala.meta._
import scalafix.internal.util.SemanticCtxImpl
import org.scalameta.logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException
import scala.meta.internal.inputs.XtensionPositionFormatMessage

abstract class SemanticRewriteSuite(
    val semanticCtx: SemanticCtx,
    val inputSourceroot: Seq[AbsolutePath],
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends FunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>
  def this(
      sctx: SemanticCtx,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this(
    sctx,
    List(inputSourceroot),
    expectedOutputSourceroot
  )
  def this(
      database: Database,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) =
    this(
      new SemanticCtxImpl(database),
      inputSourceroot,
      expectedOutputSourceroot
    )

  private def dialectToPath(dialect: String): Option[String] =
    Option(dialect).collect {
      case "Scala211" => "scala-2.11"
      case "Scala212" => "scala-2.12"
    }

  def runOn(diffTest: DiffTest): Unit = {
    test(diffTest.name) {
      val (rewrite, config) = diffTest.config.apply()
      val ctx = RewriteCtx(
        config.dialect(diffTest.original).parse[Source].get,
        config.copy(dialect = diffTest.attributes.dialect)
      )
      val patch = rewrite.rewrite(ctx)
      val obtainedWithComment = Patch.apply(patch, ctx, rewrite.semanticOption)
      val lintMessages = Patch.lintMessages(patch, ctx).to[mutable.Set]
      def assertLint(position: Position, key: String): Unit = {
        val matchingMessage = lintMessages.find { m =>
          // NOTE(olafur) I have no idea why -1 is necessary.
          m.position.startLine == (position.startLine - 1) &&
          m.category.key(rewrite.rewriteName) == key
        }
        matchingMessage match {
          case Some(x) =>
            lintMessages -= x
          case None =>
            throw new TestFailedException(
              position.formatMessage(
                "error",
                s"Message '$key' was not reported here!"),
              0
            )
        }
      }
      val obtained = {
        val tokens = obtainedWithComment.tokenize.get
        val configComment = tokens.find { x =>
          x.is[Token.Comment] && x.syntax.startsWith("/*")
        }.get
        val LintAssertion = " scalafix: (.*)".r
        tokens.filter {
          case `configComment` => false
          case tok @ Token.Comment(LintAssertion(key)) =>
            assertLint(tok.pos, key)
            false
          case _ => true
        }.mkString
      }
      if (lintMessages.nonEmpty) {
        lintMessages.foreach(x => ctx.printLintMessage(x, rewrite.rewriteName))
        val key = lintMessages.head.category.key(rewrite.rewriteName)
        val explanation =
          s"""|To fix this problem, suffix the culprit lines with
              |   // scalafix: $key
              |""".stripMargin
        throw new TestFailedException(
          s"Uncaught linter messages! $explanation",
          0)
      }
      val candidateOutputFiles = expectedOutputSourceroot.flatMap { root =>
        val scalaSpecificFilename =
          dialectToPath(diffTest.attributes.language).toList.map(path =>
            root.resolve(RelativePath(
              diffTest.filename.toString().replaceFirst("scala", path))))
        root.resolve(diffTest.filename) :: scalaSpecificFilename
      }
      val candidateBytes = candidateOutputFiles
        .collectFirst { case f if f.isFile => f.readAllBytes }
        .getOrElse {
          val tried = candidateOutputFiles.mkString("\n")
          sys.error(
            s"""Missing expected output file for test ${diffTest.filename}. Tried:
               |$tried""".stripMargin)
        }
      val expected = new String(
        candidateBytes
      )
      assertNoDiff(obtained, expected)
    }
  }

  /** Helper method to print out semanticCtx for individual files */
  def debugFile(filename: String): Unit = {
    semanticCtx.entries.foreach { entry =>
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
    DiffTest.testToRun(DiffTest.fromSemanticCtx(semanticCtx))
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}
