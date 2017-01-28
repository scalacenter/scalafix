package scalafix.rewrite

import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.util.DiffAssertions
import scala.collection.immutable.Seq

import org.scalatest.FunSuiteLike

class RewriteSuite(rewrite: Rewrite) extends FunSuiteLike with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
      val obtained =
        Scalafix.fix(original, ScalafixConfig(rewrites = Seq(rewrite))).get
      assertNoDiff(obtained, expected)
    }
  }

}
