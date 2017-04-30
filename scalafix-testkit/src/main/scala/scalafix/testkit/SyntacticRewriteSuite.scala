package scalafix
package testkit

import scalafix.syntax._
import scala.meta._
import scala.collection.immutable.Seq
import scalafix.Scalafix
import scalafix.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx

import org.scalatest.FunSuiteLike

class SyntacticRewriteSuite(rewrite: Rewrite)
    extends FunSuiteLike
    with DiffAssertions {
  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
      val obtained =
        Scalafix
          .fix(Input.String(original), ScalafixConfig(rewrite = rewrite))
          .get
      assertNoDiff(obtained, expected)
    }
  }
  def checkDiff(original: Input, expected: String): Unit = {
    test(original.label) {
      val ctx = RewriteCtx(original.parse[Source].get, ScalafixConfig.default)
      val obtained = rewrite.diff(ctx)
      assert(obtained == expected)
    }
  }
}
