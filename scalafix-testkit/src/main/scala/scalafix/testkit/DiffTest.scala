package scalafix.testkit

import scala.meta._
import scalafix.SemanticdbIndex
import scalafix.Rule
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.ScalafixConfig
import scalafix.reflect.ScalafixReflect
import org.scalatest.exceptions.TestFailedException

case class DiffTest(
    filename: RelativePath,
    original: Input,
    document: Document,
    config: () => (Rule, ScalafixConfig),
    isSkip: Boolean,
    isOnly: Boolean) {
  def name: String = filename.toString()
  def originalStr = new String(original.chars)
}

object DiffTest {

  private val PrefixRegex = "\\s+(ONLY|SKIP)".r
  private def stripPrefix(str: String) = PrefixRegex.replaceFirstIn(str, "")

  def fromSemanticdbIndex(index: SemanticdbIndex): Seq[DiffTest] =
    index.documents.map { document =>
      val input @ Input.VirtualFile(label, code) = document.input
      val relpath = RelativePath(label)
      val config: () => (Rule, ScalafixConfig) = { () =>
        input.tokenize.get
          .collectFirst {
            case Token.Comment(comment) =>
              val decoder =
                ScalafixReflect.fromLazySemanticdbIndex(
                  LazySemanticdbIndex(_ => Some(index)))
              ScalafixConfig
                .fromInput(
                  metaconfig.Input.VirtualFile(label, stripPrefix(comment)),
                  LazySemanticdbIndex(_ => Some(index)))(decoder)
                .get
          }
          .getOrElse(throw new TestFailedException(
            s"Missing scalafix configuration inside comment at top of file $relpath",
            0))
      }
      DiffTest(
        filename = relpath,
        original = input,
        document = document,
        config = config,
        isSkip = code.contains("SKIP"),
        isOnly = code.contains("ONLY")
      )
    }

  def testToRun(tests: Seq[DiffTest]): Seq[DiffTest] = {
    val onlyOne = tests.exists(_.isOnly)
    def testShouldRun(t: DiffTest): Boolean = !onlyOne || t.isOnly
    tests.filter(testShouldRun)
  }

}
