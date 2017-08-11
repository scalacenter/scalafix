package scalafix.testkit

import scala.meta._
import scalafix.SemanticCtx
import scalafix.Rewrite
import scalafix.internal.config.ScalafixConfig
import scalafix.reflect.ScalafixReflect
import org.scalatest.exceptions.TestFailedException

case class DiffTest(
    filename: RelativePath,
    original: Input,
    attributes: Attributes,
    config: () => (Rewrite, ScalafixConfig),
    isSkip: Boolean,
    isOnly: Boolean) {
  def name: String = filename.toString()
  def originalStr = new String(original.chars)
}

object DiffTest {

  private val PrefixRegex = "\\s+(ONLY|SKIP)".r
  private def stripPrefix(str: String) = PrefixRegex.replaceFirstIn(str, "")

  def fromSemanticCtx(semanticCtx: SemanticCtx): Seq[DiffTest] = semanticCtx.entries.map {
    attributes =>
      val input @ Input.VirtualFile(label, code) = attributes.input
      val relpath = RelativePath(label)
      val config: () => (Rewrite, ScalafixConfig) = { () =>
        input.tokenize.get
          .collectFirst {
            case Token.Comment(comment) =>
              val decoder =
                ScalafixReflect.fromLazySemanticCtx(_ => Some(semanticCtx))
              ScalafixConfig
                .fromInput(
                  Input.VirtualFile(label, stripPrefix(comment)),
                  _ => Some(semanticCtx))(decoder)
                .get
          }
          .getOrElse(throw new TestFailedException(
            s"Missing scalafix configuration inside comment at top of file $relpath",
            0))
      }
      DiffTest(
        filename = relpath,
        original = input,
        attributes = attributes,
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
