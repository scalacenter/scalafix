package scalafix.testkit

import scalafix.internal.testkit.DiffAssertions
import org.scalameta.FileLine

/**
 **/
trait BaseScalafixSuite {
  class Fail(msg: String) extends Exception(msg)
  def scalafixTest(name: String)(fun: => Any)(implicit pos: FileLine): Unit
  def assertNoDiff(
      obtained: String,
      expected: String,
      title: String = ""
  ): Boolean = DiffAssertions.assertNoDiff(obtained, expected, title)
}
