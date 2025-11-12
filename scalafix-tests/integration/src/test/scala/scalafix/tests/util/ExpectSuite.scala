package scalafix.tests.util
import java.nio.charset.StandardCharsets

import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

import org.scalatest.funsuite.AnyFunSuite
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scalafix.tests.core.BaseSemanticSuite
import scalafix.v1.SemanticDocument

trait ExpectSuite extends AnyFunSuite with DiffAssertions {
  def filename: String
  def obtained(): String

  final def path: AbsolutePath = {
    def expectedPath(suffix: String) =
      AbsolutePath(s"${BuildInfo.resourceDirectory}-${suffix}")
        .resolve("expect")
        .resolve(filename.stripSuffix("Test.scala") + ".expect")

    val versionPrefixesLongestFirst =
      BuildInfo.scalaVersion.split("\\.").inits.map(_.mkString("."))

    versionPrefixesLongestFirst
      .map(expectedPath)
      .find(_.toFile.exists())
      .get
  }
  final implicit lazy val sdoc: SemanticDocument =
    BaseSemanticSuite.loadDoc(filename)
  final def expected(): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)

  test(filename) {
    assertNoDiff(obtained(), expected())
  }

}
