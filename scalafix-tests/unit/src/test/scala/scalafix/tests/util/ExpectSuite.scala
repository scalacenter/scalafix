package scalafix.tests.util
import java.nio.charset.StandardCharsets
import org.scalatest.FunSuite
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scalafix.tests.core.BaseSemanticSuite
import scalafix.v1.SemanticDocument

trait ExpectSuite extends FunSuite with DiffAssertions {
  def filename: String
  def obtained(): String

  final def path: AbsolutePath =
    AbsolutePath(BuildInfo.unitResourceDirectory)
      .resolve("expect")
      .resolve(filename.stripSuffix("Test.scala") + ".expect")
  final implicit lazy val sdoc: SemanticDocument =
    BaseSemanticSuite.loadDoc(filename)
  final def expected(): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)

  test(filename) {
    assertNoDiff(obtained(), expected())
  }

}
