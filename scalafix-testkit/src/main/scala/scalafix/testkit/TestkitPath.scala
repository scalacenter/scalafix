package scalafix.testkit

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import scala.meta.inputs.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scalafix.internal.v1._

/**
  * An input file for a testkit test.
  *
  * @param input the absolute path to the input file.
  * @param testPath the input file relativized by the input source directory.
  *                 Used to compute the test name and the expected output file.
  * @param semanticdbPath the input file relativized by the SemanticDB sourceroot.
  *                       Used to compute the path to the SemanticDB payload.
  */
final class TestkitPath(
    val input: AbsolutePath,
    val testPath: RelativePath,
    val semanticdbPath: RelativePath
) {
  override def toString: String = {
    val map = Map(
      "input" -> input,
      "testPath" -> testPath,
      "semanticdbPath" -> semanticdbPath
    )
    pprint.PPrinter.BlackWhite.tokenize(map).mkString
  }
  def testName: String = testPath.toRelativeURI.toString
  def toInput: Input =
    Input.VirtualFile(testName, FileIO.slurp(input, StandardCharsets.UTF_8))
  def resolveOutput(props: TestkitProperties): Either[String, AbsolutePath] = {
    val candidates =
      props.outputSourceDirectories.map(dir => dir.resolve(testPath))
    def tried: String = candidates.mkString("\n  ", "\n  ", "")
    candidates.filter(_.isFile) match {
      case head :: Nil =>
        Right(head)
      case Nil =>
        Left(s"Missing output file for $testPath: $tried")
      case _ =>
        Left(s"Ambiguous output file for $testPath: $tried")
    }
  }
}

object TestkitPath {
  private val isScalaFile =
    FileSystems.getDefault.getPathMatcher("glob:**.scala")
  def fromProperties(props: TestkitProperties): List[TestkitPath] = {
    props.inputSourceDirectories.flatMap { sourceDirectory =>
      val ls = FileIO.listAllFilesRecursively(sourceDirectory)
      val scalaFiles = ls.files.filter(path => isScalaFile.matches(path.toNIO))
      scalaFiles.map { testPath =>
        val input = sourceDirectory.resolve(testPath)
        val semanticdbPath = input.toRelative(props.sourceroot)
        new TestkitPath(input, testPath, semanticdbPath)
      }
    }
  }
}
