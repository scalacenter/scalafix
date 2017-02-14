package scalafix.rewrite

import scalafix.Fixed
import scalafix.Scalafix
import scalafix.util.DiffAssertions
import scala.collection.immutable.Seq
import scalafix.config.ScalafixConfig

import org.scalatest.FunSuiteLike

class RewriteSuite(rewrite: ScalafixRewrite)
    extends FunSuiteLike
    with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
      val obtained =
        Scalafix.fix(original, ScalafixConfig(rewrites = List(rewrite))).get
      assertNoDiff(obtained, expected)
    }
  }

}
