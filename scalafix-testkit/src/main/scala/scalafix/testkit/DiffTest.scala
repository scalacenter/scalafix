package scalafix.testkit

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.Rewrite
import scalafix.config.ScalafixConfig
import scalafix.reflect.ScalafixReflect
import org.scalatest.exceptions.TestFailedException

case class DiffTest(filename: RelativePath,
                    original: Input,
                    attributes: Attributes,
                    config: () => (Rewrite, ScalafixConfig),
                    isSkip: Boolean,
                    isOnly: Boolean) {
  def name: String = filename.toString()
  private def prefix =
    if (isOnly) "ONLY "
    else if (isSkip) "SKIP "
    else ""
  def originalStr = new String(original.chars)
}

object DiffTest {

  private val PrefixRegex = "\\s+(ONLY|SKIP)".r
  private def stripPrefix(str: String) = PrefixRegex.replaceFirstIn(str, "")

  def fromMirror(mirror: Database): Seq[DiffTest] = mirror.entries.collect {
    case (input @ Input.LabeledString(label, code), attributes) =>
      val relpath = RelativePath(label)
      val config: () => (Rewrite, ScalafixConfig) = { () =>
        input.tokenize.get
          .collectFirst {
            case Token.Comment(comment) =>
              val decoder =
                ScalafixReflect.fromLazyMirror(_ => Some(mirror))
              ScalafixConfig
                .fromInput(Input.LabeledString(label, stripPrefix(comment)),
                           _ => Some(mirror))(decoder)
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
