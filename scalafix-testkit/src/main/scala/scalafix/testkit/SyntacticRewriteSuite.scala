package scalafix
package testkit

import scala.meta._
import scalafix.internal.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._

import org.scalatest.FunSuiteLike

class SyntacticRewriteSuite(rewrite: Rewrite)
    extends FunSuiteLike
    with DiffAssertions {
  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
      val obtained = rewrite.apply(Input.String(original))
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
