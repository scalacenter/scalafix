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
    val versionParts = BuildInfo.scalaVersion.split("\\.")
    val scalaMajorVersion = versionParts(0)
    val scalaMinorVersion = if (versionParts.length > 1) versionParts(1) else "0"
    val scalaMinorMajorVersion = s"${scalaMajorVersion}.${scalaMinorVersion}"
    
    val expectFileName = filename.stripSuffix("Test.scala") + ".expect"
    
    // Try minor version specific directory first (e.g., resources-3.7)
    val minorVersionPath = AbsolutePath(s"${BuildInfo.resourceDirectory}-${scalaMinorMajorVersion}")
      .resolve("expect")
      .resolve(expectFileName)
    
    if (minorVersionPath.toFile.exists()) {
      minorVersionPath
    } else {
      // Fall back to major version directory (e.g., resources-3)
      AbsolutePath(s"${BuildInfo.resourceDirectory}-${scalaMajorVersion}")
        .resolve("expect")
        .resolve(expectFileName)
    }
  }
  final implicit lazy val sdoc: SemanticDocument =
    BaseSemanticSuite.loadDoc(filename)
  final def expected(): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)

  test(filename) {
    assertNoDiff(obtained(), expected())
  }

}
