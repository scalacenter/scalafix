package scalafix
package testkit

import scalafix.syntax._
import scala.meta._
import scalafix.internal.util.SemanticCtxImpl
import org.scalameta.logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

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
      val obtainedWithComment =
        rewrite.apply(
          diffTest.original,
          config.copy(dialect = diffTest.attributes.dialect))
      val obtained = {
        val tokens = obtainedWithComment.tokenize.get
        val comment = tokens
          .find(x => x.is[Token.Comment] && x.syntax.startsWith("/*"))
        tokens.filterNot(comment.contains).mkString
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
