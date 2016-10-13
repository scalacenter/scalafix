package scalafix.rewrite

import scalafix.Scalafix
import scalafix.config.ScalafixConfig
import scalafix.util.DiffAssertions

import org.scalatest.FunSuiteLike

class RewriteSuite(rewrite: Rewrite) extends FunSuiteLike with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      import scala.meta._
      Scalafix.fix(original, ScalafixConfig(rewrites = Seq(rewrite))) match {
        case Right(obtained) =>
          assertNoDiff(obtained, expected)
        case Left(e) =>
          throw e
      }
    }
  }

}
