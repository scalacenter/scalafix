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

  private def dialectToPath(dialect: Dialect): Option[String] =
    Option(dialect).collect {
      case dialects.Scala211 => "scala-2.11"
      case dialects.Scala212 => "scala-2.12"
    }

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
      val candidateOutputFiles = expectedOutputSourceroot.flatMap { root =>
        val scalaSpecificFilename =
          dialectToPath(diffTest.attributes.dialect).toList.map(path =>
            root.resolve(RelativePath(
              diffTest.filename.value.replaceFirst("scala", path))))
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
