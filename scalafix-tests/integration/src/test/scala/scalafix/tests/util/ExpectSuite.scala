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
    val scalaMajorMinorVersion =
      BuildInfo.scalaVersion.split('.').take(2).mkString(".")
    AbsolutePath(s"${BuildInfo.resourceDirectory}-${scalaMajorMinorVersion}")
      .resolve("expect")
      .resolve(filename.stripSuffix("Test.scala") + ".expect")
  }
  final implicit lazy val sdoc: SemanticDocument =
    BaseSemanticSuite.loadDoc(filename)
  final def expected(): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)

  test(filename) {
    assertNoDiff(obtained(), expected())
  }

}
