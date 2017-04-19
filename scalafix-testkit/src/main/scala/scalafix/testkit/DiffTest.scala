package scalafix.testkit

import scala.collection.immutable.Seq
import scala.meta.io.AbsolutePath
import scalafix.config.ScalafixConfig
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.rewrite.ScalafixMirror
import scalafix.util.FileOps

import java.io.File

import metaconfig.Configured
import org.scalameta.logger

case class DiffTest(spec: String,
                    name: String,
                    filename: String,
                    original: String,
                    expected: String,
                    skip: Boolean,
                    only: Boolean,
                    config: Option[ScalafixMirror] => ScalafixConfig) {
  def noWrap: Boolean = name.startsWith("NOWRAP ")
  def checkSyntax: Boolean = spec.startsWith("checkSyntax")
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

object DiffTest {
  def fromFile(directory: File): Seq[DiffTest] = {
    val tests: Seq[DiffTest] = {
      for {
        filename <- getTestFiles(directory)
        test <- {
          val content = FileOps.readFile(filename)
          DiffTest(directory, content, filename)
        }
      } yield test
    }
    val onlyOne = tests.exists(_.only)
    def testShouldRun(t: DiffTest): Boolean = !onlyOne || t.only
    tests.filter(testShouldRun)
  }

  private def isOnly(name: String): Boolean = name.startsWith("ONLY ")
  private def isSkip(name: String): Boolean = name.startsWith("SKIP ")
  private def stripPrefix(name: String) =
    name.stripPrefix("SKIP ").stripPrefix("ONLY ").trim
  private def apply(testDir: File,
                    content: String,
                    filename: String): Seq[DiffTest] =
    apply(filename.stripPrefix(testDir.getPath + File.separator),
          content,
          filename)

  def apply(spec: String, content: String, filename: String): Seq[DiffTest] = {
    val moduleOnly = isOnly(content)
    val moduleSkip = isSkip(content)
    val split = content.split("\n<<< ")

    val style = { mirror: Option[ScalafixMirror] =>
      val firstLine = split.head
      ScalafixConfig.fromString(firstLine, mirror)(
        ScalafixCompilerDecoder(mirror)) match {
        case Configured.Ok(x) => x
        case Configured.NotOk(x) =>
          throw new IllegalArgumentException(s"""Failed to parse $filename
                                                |Mirror: $mirror
                                                |Error: $x""".stripMargin)
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
  private def getTestFiles(directory: File): Vector[String] = {
    val testsFiles = FileOps.listFiles(directory).filter(_.endsWith(".source"))
    val onlyTests = testsFiles.filter(_.contains("\n<<< ONLY"))
    if (onlyTests.nonEmpty) onlyTests
    else testsFiles
  }

  private def bySpecThenName(left: DiffTest, right: DiffTest): Boolean = {
    import scala.math.Ordered.orderingToOrdered
    (left.spec, left.name).compare(right.spec -> right.name) < 0
  }
}
