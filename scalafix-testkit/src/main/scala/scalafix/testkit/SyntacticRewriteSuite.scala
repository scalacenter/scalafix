package scalafix.testkit

import scala.meta._
import scala.collection.immutable.Seq
import scalafix.Scalafix
import scalafix.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.ScalafixRewrite

import org.scalatest.FunSuiteLike

class SyntacticRewriteSuite(rewrite: ScalafixRewrite)
    extends FunSuiteLike
    with DiffAssertions {
  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
//      val obtained =
//        Scalafix.fix(original, ScalafixConfig(rewrites = List(rewrite))).get
//      assertNoDiff(obtained, expected)
    }
  }
}
