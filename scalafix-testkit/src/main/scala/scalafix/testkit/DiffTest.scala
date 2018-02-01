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

  def fromSemanticdbIndex(index: SemanticdbIndex): Seq[DiffTest] = {
    index.documents.flatMap { document =>
      val input = document.input
      document.input match {
        case file: Input.File => {
          val relative = index.sourcepath.shallow
            .find(sourceroot => file.path.toNIO.startsWith(sourceroot.toNIO))
            .map(root => file.path.toRelative(root))
            .getOrElse(throw new Exception(
              s"${file.path} is not relative to any sourcroot"))

          val label = relative.toString()
          val code = file.text

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
                      Input.VirtualFile(label, stripPrefix(comment)),
                      LazySemanticdbIndex(_ => Some(index)))(decoder)
                    .get
              }
              .getOrElse(throw new TestFailedException(
                s"Missing scalafix configuration inside comment at top of file $relpath",
                0))
          }
          Seq(
            DiffTest(
              filename = relpath,
              original = input,
              document = document,
              config = config,
              isSkip = code.contains("SKIP"),
              isOnly = code.contains("ONLY")
            ))
        }
        case Input.VirtualFile(label, _) =>
          println(s"ignoring: $label")
          // I don't know why, we get those as VirtualFile:
          // ignoring: scalafix-tests/shared/src/main/scala/test/SymbolMatcherTest.scala
          // ignoring: scalafix-tests/shared/src/main/scala/test/DenotationOpsTest.scala
          // ignoring: scalafix-tests/shared/src/main/scala/test/SemanticdbIndexTest.scala
          // ignoring: scala/sbtfix/Sbt1.scala

          Seq()

        case other =>
          throw new Exception(s"unexpected $other")
      }
    }
  }

  def testToRun(tests: Seq[DiffTest]): Seq[DiffTest] = {
    val onlyOne = tests.exists(_.isOnly)
    def testShouldRun(t: DiffTest): Boolean = !onlyOne || t.isOnly
    tests.filter(testShouldRun)
  }

}
