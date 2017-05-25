package scalafix
package testkit

import scala.meta._
import scala.meta.internal.io.FileIO
import scalafix.reflect.ScalafixCompilerDecoder
import metaconfig.Configured
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException

abstract class SemanticRewriteSuite(
    val mirror: Database,
    val inputSourceroot: AbsolutePath,
    val expectedOutputSourceroot: AbsolutePath
) extends FunSuite
    with DiffAssertions { self =>
  mirror.entries.foreach {
    case (input @ Input.LabeledString(relpath, code), attributes)
        if relpath.contains("test") =>
      test(relpath) {
        val (rewrite, config) = input.tokenize.get
          .collectFirst {
            case Token.Comment(comment) =>
              val decoder =
                ScalafixCompilerDecoder.fromMirrorOption(Some(mirror))
              ScalafixConfig
                .fromInput(Input.LabeledString(relpath, comment),
                           Some(mirror))(decoder)
                .get
          }
          .getOrElse(throw new TestFailedException(
            s"Missing scalafix configuration inside comment at top of file $relpath",
            0))
        val obtainedWithComment =
          rewrite.apply(input, config.copy(dialect = attributes.dialect))
        val obtained = {
          val tokens = obtainedWithComment.tokenize.get
          val comment = tokens
            .find(x => x.is[Token.Comment] && x.syntax.startsWith("/*"))
            .get
          tokens.filterNot(_ eq comment).mkString
        }
        val expected =
          new String(expectedOutputSourceroot.resolve(relpath).readAllBytes)
        assertNoDiff(obtained, expected)
      }
    case els => // do nothing
  }
}
