package scalafix
package testkit

import scala.meta._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

abstract class SemanticRewriteSuite(
    val mirror: Database,
    val inputSourceroot: AbsolutePath,
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends FunSuite
    with DiffAssertions
    with BeforeAndAfterAll { self =>
  def runOn(diffTest: DiffTest): Unit = {
    test(diffTest.name) {
      val (rewrite, config) = diffTest.config.apply()
      val obtainedWithComment =
        rewrite.apply(diffTest.original,
                      config.copy(dialect = diffTest.attributes.dialect))
      val obtained = {
        val tokens = obtainedWithComment.tokenize.get
        val comment = tokens
          .find(x => x.is[Token.Comment] && x.syntax.startsWith("/*"))
          .get
        tokens.filter(_ ne comment).mkString
      }
      val expected =
        new String(
          expectedOutputSourceroot
            .map(_.resolve(diffTest.filename))
            .find(_.isFile)
            .map(_.readAllBytes)
            .getOrElse {
              val tried = expectedOutputSourceroot
                .map(_.resolve(diffTest.filename))
                .mkString("\n")
              sys.error(
                s"""Missing expected output file for test ${diffTest.filename}. Tried:
                   |$tried""".stripMargin)
            }
        )
      assertNoDiff(obtained, expected)
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
  lazy val testsToRun = DiffTest.testToRun(DiffTest.fromMirror(mirror))
  def runAllTests(): Unit = {
    testsToRun.foreach(runOn)
  }
}
