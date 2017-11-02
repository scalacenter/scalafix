package scalafix.internal.testkit

import scala.meta._
import scalafix.Rule
import scalafix.SemanticdbIndex
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.ScalafixConfig
import scalafix.reflect.ScalafixReflect

case class DiffTest(
    filename: RelativePath,
    original: Input,
    document: Document,
    config: () => (Rule, ScalafixConfig),
    // Will be removed in 0.6.0
    isSkip: Boolean,
    // Will be removed in 0.6.0
    isOnly: Boolean) {
  def name: String = filename.toString()
  def originalStr = new String(original.chars)
}

object DiffTest {

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
                  Input.VirtualFile(label, comment),
                  LazySemanticdbIndex(_ => Some(index)))(decoder)
                .get
          }
          .getOrElse(throw new Exception(
            s"Missing scalafix configuration inside comment at top of file $relpath"
          ))
      }
      DiffTest(
        filename = relpath,
        original = input,
        document = document,
        config = config,
        isSkip = false,
        isOnly = false
      )
    }

  @deprecated("Does nothing, will be removed in next release", "0.5.4")
  def testToRun(tests: Seq[DiffTest]): Seq[DiffTest] = {
    tests
  }

}
