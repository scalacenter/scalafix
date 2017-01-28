package scalafix

import scala.collection.immutable.Seq
import scalafix.rewrite.ExplicitImplicit
import scalafix.rewrite.ProcedureSyntax
import scalafix.util.FileOps
import scalafix.util.logger

import java.io.File

object DiffTest {
  def testsToRun: Seq[DiffTest] = tests.filter(testShouldRun)

  private val testDir = "scalafix-nsc/src/test/resources"
  private def isOnly(name: String): Boolean = name.startsWith("ONLY ")
  private def isSkip(name: String): Boolean = name.startsWith("SKIP ")
  private def stripPrefix(name: String) =
    name.stripPrefix("SKIP ").stripPrefix("ONLY ").trim
  private def apply(content: String, filename: String): Seq[DiffTest] = {
    val spec = filename.stripPrefix(testDir + File.separator)
    val moduleOnly = isOnly(content)
    val moduleSkip = isSkip(content)
    val split = content.split("\n<<< ")

    val style: ScalafixConfig = {
      val firstLine = split.head
      ScalafixConfig.fromString(firstLine) match {
        case Right(x) => x
        case Left(e) => throw new IllegalArgumentException(e)
      }
    }

    split.tail.map { t =>
      val before :: expected :: Nil = t.split("\n>>>\n", 2).toList
      val name :: original :: Nil = before.split("\n", 2).toList
      val actualName = stripPrefix(name)
      DiffTest(spec,
               actualName,
               filename,
               original,
               expected,
               moduleSkip || isSkip(name),
               moduleOnly || isOnly(name),
               style)
    }.toList
  }

  /** Avoids parsing all files if some tests are marked ONLY. */
  private def getTestFiles: Vector[String] = {
    val testsFiles = FileOps.listFiles(testDir).filter(_.endsWith(".source"))
    val onlyTests = testsFiles.filter(_.contains("\n<<< ONLY"))
    if (onlyTests.nonEmpty) onlyTests
    else testsFiles
  }

  private lazy val tests: Seq[DiffTest] = {
    for {
      filename <- getTestFiles
      test <- {
        val content = FileOps.readFile(filename)
        DiffTest(content, filename)
      }
    } yield test
  }
  private lazy val onlyOne = tests.exists(_.only)

  private def testShouldRun(t: DiffTest): Boolean = !onlyOne || t.only
  private def bySpecThenName(left: DiffTest, right: DiffTest): Boolean = {
    import scala.math.Ordered.orderingToOrdered
    (left.spec, left.name).compare(right.spec -> right.name) < 0
  }
}

case class DiffTest(spec: String,
                    name: String,
                    filename: String,
                    original: String,
                    expected: String,
                    skip: Boolean,
                    only: Boolean,
                    config: ScalafixConfig) {
  def noWrap: Boolean = name.startsWith("NOWRAP ")
  def isSyntax: Boolean =
    Seq(
      "syntactic",
      ProcedureSyntax.toString
    ).exists(spec.startsWith)

  private def packageName = name.replaceAll("[^a-zA-Z0-9]", "")
  private def packagePrefix = s"package $packageName {\n"
  private def packageSuffix = s" }\n"
  def wrapped(code: String = original): String =
    if (noWrap) original
    else s"$packagePrefix$code$packageSuffix"
  def unwrap(code: String): String =
    if (noWrap) code
    else code.stripPrefix(packagePrefix).stripSuffix(packageSuffix)

  val fullName = s"$spec: $name"
}
